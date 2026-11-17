package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MapsLocalizacaoAdapterResilienceTest {

    private final FakeMapsClient mapsClient = mock(FakeMapsClient.class);

    private final MapsLocalizacaoAdapter adapter =
            new MapsLocalizacaoAdapter(mapsClient);

    @Test
    void deveRepetirBuscaDeCoordenadasAposFalhaTransitoria() {
        Endereco endereco = endereco();

        when(mapsClient.buscarCoordenadas(endereco))
                .thenThrow(new FalhaTemporariaMapsException("Timeout"))
                .thenReturn(new FakeMapsClient.MapCoordinates(
                        "-25.1",
                        "-50.15",
                        "EXACT"
                ));

        Localizacao resultado = adapter.buscarCoordenadas(endereco);

        assertThat(resultado.getLatitude()).isEqualTo(-25.1);
        assertThat(resultado.getLongitude()).isEqualTo(-50.15);

        verify(mapsClient, times(2)).buscarCoordenadas(endereco);
    }

    @Test
    void deveRepetirCalculoDeRotaAposFalhaTransitoria() {
        Localizacao origem = new Localizacao(-25.1, -50.15);
        Localizacao destino = new Localizacao(-25.095, -50.16);

        when(mapsClient.calcularRota(origem, destino))
                .thenThrow(new FalhaTemporariaMapsException("Serviço temporariamente indisponível"))
                .thenReturn(new FakeMapsClient.RouteResult(
                        1.32,
                        17,
                        "CENTRAL"
                ));

        Rota resultado = adapter.calcularRota(origem, destino);

        assertThat(resultado)
                .isEqualTo(new Rota(1.32, 17, "CENTRAL"));

        verify(mapsClient, times(2)).calcularRota(origem, destino);
    }

    @Test
    void deveInterromperAposTresTentativasSemSucesso() {
        Endereco endereco = endereco();

        when(mapsClient.buscarCoordenadas(endereco))
                .thenThrow(new FalhaTemporariaMapsException(
                        "Provedor de mapas indisponível"
                ));

        assertThatThrownBy(() -> adapter.buscarCoordenadas(endereco))
                .isInstanceOf(FalhaTemporariaMapsException.class)
                .hasMessage("Provedor de mapas indisponível");

        verify(mapsClient, times(3)).buscarCoordenadas(endereco);
    }

    @Test
    void naoDeveRepetirOperacaoQuandoErroNaoForTransitorio() {
        Endereco endereco = endereco();

        when(mapsClient.buscarCoordenadas(endereco))
                .thenReturn(new FakeMapsClient.MapCoordinates(
                        "coordenada-invalida",
                        "-50.15",
                        "EXACT"
                ));

        assertThatThrownBy(() -> adapter.buscarCoordenadas(endereco))
                .isInstanceOf(NumberFormatException.class);

        verify(mapsClient, times(1)).buscarCoordenadas(endereco);
    }

    private Endereco endereco() {
        return new Endereco(
                "Rua Integração",
                "1",
                "Centro",
                "Ponta Grossa",
                "84000-000",
                new Localizacao(-25.1, -50.15)
        );
    }
}