package br.edu.foodnow.integration;

import br.edu.foodnow.geo.CoordenadasResolvidas;
import br.edu.foodnow.geo.ProvedorGeografico;
import br.edu.foodnow.geo.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Component;

/**
 * Adapter que traduz o contrato do {@link FakeMapsClient} (strings, records do provedor)
 * para a porta {@link ProvedorGeografico}. É o único ponto da aplicação que conhece o
 * {@code FakeMapsClient}.
 */
@Component
public class MapsProvedorGeografico implements ProvedorGeografico {
    private static final String PRECISAO_APROXIMADA = "APPROXIMATE";

    private final FakeMapsClient mapsClient;

    public MapsProvedorGeografico(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @Override
    public CoordenadasResolvidas buscarCoordenadas(Endereco endereco) {
        FakeMapsClient.MapCoordinates resposta = mapsClient.buscarCoordenadas(endereco);
        Localizacao localizacao = new Localizacao(Double.parseDouble(resposta.lat()),
                Double.parseDouble(resposta.lng()));
        return new CoordenadasResolvidas(localizacao, PRECISAO_APROXIMADA.equals(resposta.precision()));
    }

    @Override
    public Rota calcularRota(Localizacao origem, Localizacao destino) {
        FakeMapsClient.RouteResult resposta = mapsClient.calcularRota(origem, destino);
        return new Rota(resposta.distanceKm(), resposta.durationMinutes(), resposta.deliveryZone());
    }
}
