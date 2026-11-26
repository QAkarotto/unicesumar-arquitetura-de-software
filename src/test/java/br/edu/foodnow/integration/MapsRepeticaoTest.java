package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MapsRepeticaoTest {
    private final FakeMapsClient cliente = mock(FakeMapsClient.class);
    private final MapsLocalizacaoAdapter adapter = new MapsLocalizacaoAdapter(cliente);
    private final Localizacao origem = new Localizacao(-25.100, -50.150);
    private final Localizacao destino = new Localizacao(-25.095, -50.160);
    private final Endereco endereco = new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa",
            "84000-000", destino);

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void deveRecuperarConsultaDeRotaAteATerceiraTentativa(int falhas) {
        var resposta = new FakeMapsClient.RouteResult(1.32, 17, "CENTRAL");
        int[] chamadas = {0};
        when(cliente.calcularRota(origem, destino)).thenAnswer(invocation -> {
            if (chamadas[0]++ < falhas) {
                throw new MapsIndisponivelException("Falha temporária simulada");
            }
            return resposta;
        });

        assertThat(adapter.calcularRota(origem, destino)).isEqualTo(new Rota(1.32, 17, "CENTRAL"));
        verify(cliente, times(falhas + 1)).calcularRota(origem, destino);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void deveRecuperarConsultaDeCoordenadasAteATerceiraTentativa(int falhas) {
        int[] chamadas = {0};
        when(cliente.buscarCoordenadas(endereco)).thenAnswer(invocation -> {
            if (chamadas[0]++ < falhas) {
                throw new MapsIndisponivelException("Falha temporária simulada");
            }
            return new FakeMapsClient.MapCoordinates("-25.095", "-50.160", "EXACT");
        });

        var resultado = adapter.buscarCoordenadas(endereco);

        assertThat(resultado.getLatitude()).isEqualTo(-25.095);
        assertThat(resultado.getLongitude()).isEqualTo(-50.160);
        verify(cliente, times(falhas + 1)).buscarCoordenadas(endereco);
    }

    @Test
    void deveEncerrarRotaAposTresFalhasPreservandoUltimaExcecao() {
        var primeira = new MapsIndisponivelException("Primeira falha");
        var segunda = new MapsIndisponivelException("Segunda falha");
        var ultima = new MapsIndisponivelException("Terceira falha");
        when(cliente.calcularRota(origem, destino)).thenThrow(primeira, segunda, ultima);

        assertThatThrownBy(() -> adapter.calcularRota(origem, destino)).isSameAs(ultima);
        verify(cliente, times(3)).calcularRota(origem, destino);
    }

    @Test
    void deveEncerrarCoordenadasAposTresFalhas() {
        var falha = new MapsIndisponivelException("Provedor indisponível");
        when(cliente.buscarCoordenadas(endereco)).thenThrow(falha);

        assertThatThrownBy(() -> adapter.buscarCoordenadas(endereco)).isSameAs(falha);
        verify(cliente, times(3)).buscarCoordenadas(endereco);
    }

    @Test
    void naoDeveRepetirErroNaoTransitorioDoProvedor() {
        var falha = new IllegalArgumentException("Requisição inválida");
        when(cliente.calcularRota(origem, destino)).thenThrow(falha);

        assertThatThrownBy(() -> adapter.calcularRota(origem, destino)).isSameAs(falha);
        verify(cliente).calcularRota(origem, destino);
    }

    @Test
    void naoDeveRepetirRespostaComCoordenadasMalformadas() {
        when(cliente.buscarCoordenadas(endereco)).thenReturn(
                new FakeMapsClient.MapCoordinates("latitude-invalida", "-50.160", "EXACT"));

        assertThatThrownBy(() -> adapter.buscarCoordenadas(endereco))
                .isInstanceOf(NumberFormatException.class);
        verify(cliente).buscarCoordenadas(endereco);
    }
}
