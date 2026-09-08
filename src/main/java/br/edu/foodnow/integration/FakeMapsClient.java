package br.edu.foodnow.integration;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.util.DistanciaUtil;
import org.springframework.stereotype.Component;

@Component
public class FakeMapsClient {

    public MapCoordinates buscarCoordenadas(Endereco endereco) {
        if (endereco.getLocalizacao() != null) {
            return new MapCoordinates(
                    Double.toString(endereco.getLocalizacao().getLatitude()),
                    Double.toString(endereco.getLocalizacao().getLongitude()),
                    "EXACT");
        }
        int hash = Math.abs(endereco.enderecoCompleto().hashCode());
        double latitude = -25.0 - (hash % 500) / 10_000.0;
        double longitude = -50.0 - (hash % 700) / 10_000.0;
        return new MapCoordinates(Double.toString(latitude), Double.toString(longitude), "APPROXIMATE");
    }

    public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
        double linhaReta = DistanciaUtil.calcularKm(origem, destino);
        double distanciaTrajeto = Math.round(linhaReta * 1.15 * 100.0) / 100.0;
        int minutos = 12 + (int) Math.ceil(distanciaTrajeto * 3.5);
        String zona = distanciaTrajeto <= 5 ? "CENTRAL" : "EXPANDIDA";
        return new RouteResult(distanciaTrajeto, minutos, zona);
    }

    public record MapCoordinates(String lat, String lng, String precision) {
    }

    public record RouteResult(double distanceKm, int durationMinutes, String deliveryZone) {
    }
}
