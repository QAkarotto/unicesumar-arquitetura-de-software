package br.edu.foodnow.service;

import br.edu.foodnow.model.Endereco;

public interface DistanceProvider {

    MapCoordinates buscarCoordenadas(Endereco endereco);

    Route calcularRota(Endereco origem, Endereco destino);

    record MapCoordinates(
            String lat,
            String lng,
            String precision
    ) {
    }

    record Route(
            double distanceKm,
            int durationMinutes,
            String deliveryZone
    ) {
    }
}