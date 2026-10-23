package br.edu.foodnow.integration;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;

public interface MapsClient {

    MapCoordinates buscarCoordenadas(Endereco endereco);

    RouteResult calcularRota(Localizacao origem, Localizacao destino);

    record MapCoordinates(String lat, String lng, String precision) {
    }

    record RouteResult(double distanceKm, int durationMinutes, String deliveryZone) {
    }
}