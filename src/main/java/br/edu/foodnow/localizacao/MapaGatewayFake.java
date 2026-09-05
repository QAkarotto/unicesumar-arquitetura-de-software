package br.edu.foodnow.localizacao;

import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Component;

/**
 * Adaptador do provedor simulado de mapas.
 *
 * <p>Concentra em um único ponto tudo o que é específico do fornecedor, a leitura das coordenadas
 * como texto e a interpretação dos rótulos {@code EXACT} e {@code APPROXIMATE}.</p>
 */
@Component
public class MapaGatewayFake implements MapaGateway {
    private static final String PRECISAO_APROXIMADA = "APPROXIMATE";

    private final FakeMapsClient mapsClient;

    public MapaGatewayFake(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @Override
    public Geocodificacao geocodificar(Endereco endereco) {
        FakeMapsClient.MapCoordinates resposta = mapsClient.buscarCoordenadas(endereco);
        Localizacao localizacao = new Localizacao(Double.parseDouble(resposta.lat()),
                Double.parseDouble(resposta.lng()));
        return new Geocodificacao(localizacao, PRECISAO_APROXIMADA.equals(resposta.precision()));
    }

    @Override
    public Rota calcularRota(Localizacao origem, Localizacao destino) {
        FakeMapsClient.RouteResult resposta = mapsClient.calcularRota(origem, destino);
        return new Rota(resposta.distanceKm(), resposta.durationMinutes(), resposta.deliveryZone());
    }
}
