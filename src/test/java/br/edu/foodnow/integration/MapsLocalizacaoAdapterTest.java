package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.service.LocalizacaoService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class MapsLocalizacaoAdapterTest {
    private final LocalizacaoService localizacao = new LocalizacaoService(new MapsLocalizacaoAdapter(new FakeMapsClient()));

    @Test
    void deveTraduzirCoordenadasERotaSemExporRecordsDoProvedor() {
        Endereco origem = endereco(new Localizacao(-25.100, -50.150));
        Endereco destino = endereco(new Localizacao(-25.095, -50.160));
        assertThat(localizacao.buscarCoordenadas(origem).getLatitude()).isEqualTo(-25.100);
        assertThat(localizacao.buscarCoordenadas(origem).getLongitude()).isEqualTo(-50.150);
        assertThat(localizacao.calcularRota(origem, destino)).isEqualTo(new Rota(1.32, 17, "CENTRAL"));
        assertThat(localizacao.calcularDistancia(origem, destino)).isEqualTo(1.32);
        assertThat(localizacao.estimarTempoEntrega(origem, destino)).isEqualTo(17);
    }

    @Test
    void devePreservarGeocodificacaoAproximadaQuandoNaoHaCoordenadas() {
        Endereco endereco = endereco(null);
        var esperado = new FakeMapsClient().buscarCoordenadas(endereco);
        var coordenadas = localizacao.buscarCoordenadas(endereco);
        assertThat(coordenadas.getLatitude()).isEqualTo(Double.parseDouble(esperado.lat()));
        assertThat(coordenadas.getLongitude()).isEqualTo(Double.parseDouble(esperado.lng()));
    }

    @Test
    void deveRepetirConsultaDeRotaQuandoPrimeiraTentativaFalha() {
        FakeMapsClient cliente = mock(FakeMapsClient.class);
        Localizacao origem = new Localizacao(-25.100, -50.150);
        Localizacao destino = new Localizacao(-25.095, -50.160);
        when(cliente.calcularRota(origem, destino))
                .thenThrow(new IllegalStateException("Serviço temporariamente indisponível"))
                .thenReturn(new FakeMapsClient.RouteResult(1.32, 17, "CENTRAL"));

        Rota rota = new MapsLocalizacaoAdapter(cliente).calcularRota(origem, destino);

        assertThat(rota).isEqualTo(new Rota(1.32, 17, "CENTRAL"));
        verify(cliente, times(2)).calcularRota(origem, destino);
    }

    @Test
    void devePropagarErroQuandoAsDuasTentativasFalham() {
        FakeMapsClient cliente = mock(FakeMapsClient.class);
        Localizacao origem = new Localizacao(-25.100, -50.150);
        Localizacao destino = new Localizacao(-25.095, -50.160);
        when(cliente.calcularRota(origem, destino))
                .thenThrow(new IllegalStateException("Serviço indisponível"));

        assertThatThrownBy(() -> new MapsLocalizacaoAdapter(cliente).calcularRota(origem, destino))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Serviço indisponível");
        verify(cliente, times(2)).calcularRota(origem, destino);
    }

    private Endereco endereco(Localizacao coordenadas) {
        return new Endereco("Rua Integração", "1", "Centro", "Ponta Grossa", "84000-000", coordenadas);
    }
}
