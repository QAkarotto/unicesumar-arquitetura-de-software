package br.edu.foodnow.service;

import br.edu.foodnow.integration.MapsClient;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizacaoServiceTest {

    @Test
    void deveFuncionarComOutraImplementacaoDeMapsClient() {
        MapsClient outroMapsClient = new MapsClient() {
            @Override
            public MapCoordinates buscarCoordenadas(Endereco endereco) {
                return new MapCoordinates("-25.09", "-50.16", "TESTE");
            }

            @Override
            public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
                return new RouteResult(4.5, 20, "TEST_ZONE");
            }
        };

        LocalizacaoService service = new LocalizacaoService(outroMapsClient);

        Endereco origem = new Endereco(
                "Rua A", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.09, -50.16)
        );

        Endereco destino = new Endereco(
                "Rua B", "20", "Centro", "Ponta Grossa", "84000-001",
                new Localizacao(-25.10, -50.17)
        );

        assertEquals(-25.09, service.buscarCoordenadas(origem).getLatitude());
        assertEquals(4.5, service.calcularDistancia(origem, destino));
        assertEquals(20, service.estimarTempoEntrega(origem, destino));
        assertEquals("TEST_ZONE", service.buscarZonaEntrega(origem, destino));
    }
}