package br.edu.foodnow.integration;

import br.edu.foodnow.geo.CoordenadasResolvidas;
import br.edu.foodnow.geo.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapsProvedorGeograficoTest {
    private final FakeMapsClient mapsClient = new FakeMapsClient();
    private final MapsProvedorGeografico adapter = new MapsProvedorGeografico(mapsClient);

    @Test
    void deveTraduzirCoordenadasExatasDoProvedor() {
        Endereco endereco = endereco(new Localizacao(-25.1, -50.15));

        CoordenadasResolvidas resultado = adapter.buscarCoordenadas(endereco);

        assertThat(resultado.aproximada()).isFalse();
        assertThat(resultado.localizacao().getLatitude()).isEqualTo(-25.1);
        assertThat(resultado.localizacao().getLongitude()).isEqualTo(-50.15);
    }

    @Test
    void deveSinalizarCoordenadasAproximadasQuandoOEnderecoNaoTemLocalizacao() {
        CoordenadasResolvidas resultado = adapter.buscarCoordenadas(endereco(null));

        assertThat(resultado.aproximada()).isTrue();
        assertThat(resultado.localizacao()).isNotNull();
    }

    @Test
    void deveTraduzirARotaDoProvedorParaOVocabularioDoDominio() {
        Localizacao origem = new Localizacao(-25.100, -50.150);
        Localizacao destino = new Localizacao(-25.150, -50.200);
        FakeMapsClient.RouteResult esperado = mapsClient.calcularRota(origem, destino);

        Rota rota = adapter.calcularRota(origem, destino);

        assertThat(rota.distanciaKm()).isEqualTo(esperado.distanceKm());
        assertThat(rota.duracaoMinutos()).isEqualTo(esperado.durationMinutes());
        assertThat(rota.zona()).isEqualTo(esperado.deliveryZone());
        assertThat(rota.zona()).isEqualTo("EXPANDIDA");
    }

    private Endereco endereco(Localizacao localizacao) {
        return new Endereco("Rua Adapter", "1", "Centro", "Ponta Grossa", "84000-000", localizacao);
    }
}
