package br.edu.foodnow.util;

import br.edu.foodnow.model.Localizacao;

public final class DistanciaUtil {
    private static final double RAIO_TERRA_KM = 6371.0;

    private DistanciaUtil() {
    }

    public static double calcularKm(Localizacao origem, Localizacao destino) {
        double latitude = Math.toRadians(destino.getLatitude() - origem.getLatitude());
        double longitude = Math.toRadians(destino.getLongitude() - origem.getLongitude());
        double a = Math.sin(latitude / 2) * Math.sin(latitude / 2)
                + Math.cos(Math.toRadians(origem.getLatitude()))
                * Math.cos(Math.toRadians(destino.getLatitude()))
                * Math.sin(longitude / 2) * Math.sin(longitude / 2);
        return RAIO_TERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
