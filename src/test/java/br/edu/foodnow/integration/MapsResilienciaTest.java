package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Localizacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class MapsResilienciaTest {
    private final FakeMapsClient mapsClient = mock(FakeMapsClient.class);
    private final MapsLocalizacaoAdapter adapter = new MapsLocalizacaoAdapter(mapsClient);
    private final Localizacao origem = new Localizacao(-25.100, -50.150);
    private final Localizacao destino = new Localizacao(-25.095, -50.160);
    private final FakeMapsClient.RouteResult resposta = new FakeMapsClient.RouteResult(1.32, 17, "CENTRAL");

    @Test
    void deveRecuperarConsultaAposFalhaTemporaria(CapturedOutput output) {
        when(mapsClient.calcularRota(origem, destino))
                .thenThrow(new ResourceAccessException("Provedor temporariamente indisponível"))
                .thenReturn(resposta);

        Rota rota = adapter.calcularRota(origem, destino);

        assertThat(rota).isEqualTo(new Rota(1.32, 17, "CENTRAL"));
        verify(mapsClient, times(2)).calcularRota(origem, destino);
        verifyNoMoreInteractions(mapsClient);
        List<String> registros = registros(output);
        assertThat(registros).hasSize(2);
        assertThat(registros.get(0)).contains("WARN", "tentativa=1 limite=3",
                "tipoFalha=ResourceAccessException resultado=nova_tentativa");
        assertThat(registros.get(1)).contains("INFO", "tentativa=2 limite=3 resultado=sucesso");
        assertThat(identificadores(registros)).hasSize(1);
    }

    @Test
    void devePermitirRecuperacaoNaUltimaTentativa(CapturedOutput output) {
        when(mapsClient.calcularRota(origem, destino))
                .thenThrow(new ResourceAccessException("Falha de comunicação"))
                .thenThrow(new ResourceAccessException("Falha de comunicação"))
                .thenReturn(resposta);

        assertThat(adapter.calcularRota(origem, destino)).isEqualTo(new Rota(1.32, 17, "CENTRAL"));

        verify(mapsClient, times(3)).calcularRota(origem, destino);
        verifyNoMoreInteractions(mapsClient);
        assertThat(registros(output)).hasSize(3);
        assertThat(output.getAll()).contains("tentativa=3 limite=3 resultado=sucesso")
                .doesNotContain("resultado=tentativas_esgotadas");
    }

    @Test
    void deveEncerrarAposTresFalhasSemRegistrarDadosSensiveis(CapturedOutput output) {
        ResourceAccessException falha = new ResourceAccessException(
                "token=SEGREDO_TESTE; email=cliente@teste.com; origem=-25.100,-50.150; destino=-25.095,-50.160");
        when(mapsClient.calcularRota(origem, destino)).thenThrow(falha);

        assertThatThrownBy(() -> adapter.calcularRota(origem, destino)).isSameAs(falha);

        verify(mapsClient, times(3)).calcularRota(origem, destino);
        verifyNoMoreInteractions(mapsClient);
        List<String> registros = registros(output);
        assertThat(registros).hasSize(3);
        assertThat(registros.get(2)).contains("ERROR", "tentativa=3 limite=3",
                "tipoFalha=ResourceAccessException resultado=tentativas_esgotadas");
        assertThat(identificadores(registros)).hasSize(1);
        assertThat(output.getAll()).doesNotContain("SEGREDO_TESTE", "cliente@teste.com", "-25.100", "-50.150",
                "-25.095", "-50.160", "token=", "email=");
    }

    @Test
    void naoDeveRepetirErrosQueNaoSejamDeComunicacao(CapturedOutput output) {
        IllegalArgumentException falha = new IllegalArgumentException("Dados inválidos do provedor");
        when(mapsClient.calcularRota(origem, destino)).thenThrow(falha);

        assertThatThrownBy(() -> adapter.calcularRota(origem, destino)).isSameAs(falha);

        verify(mapsClient).calcularRota(origem, destino);
        verifyNoMoreInteractions(mapsClient);
        assertThat(registros(output)).singleElement().satisfies(registro ->
                assertThat(registro).contains("ERROR", "tentativa=1 limite=3",
                        "tipoFalha=IllegalArgumentException resultado=falha_nao_repetivel"));
        assertThat(output.getAll()).doesNotContain("resultado=nova_tentativa");
    }

    @Test
    void devePreservarSucessoNaPrimeiraTentativaEIdentificarConsultasSeparadamente(CapturedOutput output) {
        when(mapsClient.calcularRota(origem, destino)).thenReturn(resposta);

        Rota primeira = adapter.calcularRota(origem, destino);
        Rota segunda = adapter.calcularRota(origem, destino);

        assertThat(primeira).isEqualTo(new Rota(1.32, 17, "CENTRAL"));
        assertThat(segunda).isEqualTo(primeira);
        verify(mapsClient, times(2)).calcularRota(origem, destino);
        verifyNoMoreInteractions(mapsClient);
        assertThat(registros(output)).hasSize(2).allSatisfy(registro ->
                assertThat(registro).contains("tentativa=1 limite=3 resultado=sucesso"));
        assertThat(identificadores(registros(output))).hasSize(2);
    }

    private List<String> registros(CapturedOutput output) {
        return output.getAll().lines().filter(linha -> linha.contains("operacao=calcular_rota ")).toList();
    }

    private List<String> identificadores(List<String> registros) {
        Pattern padrao = Pattern.compile("operacaoId=([a-f0-9-]{36})");
        return registros.stream().map(linha -> {
            var matcher = padrao.matcher(linha);
            assertThat(matcher.find()).as("Identificador da consulta presente no log").isTrue();
            return matcher.group(1);
        }).distinct().toList();
    }
}
