package br.edu.foodnow.integration;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.service.DistanceProvider;
import org.springframework.stereotype.Component;

@Component
public class FakeMapsDistanceProvider implements DistanceProvider {

    private final FakeMapsClient mapsClient;

    public FakeMapsDistanceProvider(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @Override
    public MapCoordinates buscarCoordenadas(Endereco endereco) {
        FakeMapsClient.MapCoordinates resposta =
                mapsClient.buscarCoordenadas(endereco);

        return new MapCoordinates(
                resposta.lat(),
                resposta.lng(),
                resposta.precision()
        );
    }

    @Override
    public Route calcularRota(Endereco origem, Endereco destino) {
        FakeMapsClient.RouteResult resposta =
                mapsClient.calcularRota(
                        origem.getLocalizacao(),
                        destino.getLocalizacao()
                );

        return new Route(
                resposta.distanceKm(),
                resposta.durationMinutes(),
                resposta.deliveryZone()
        );
    }
}