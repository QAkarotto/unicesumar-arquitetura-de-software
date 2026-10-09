package main.java.foodnow.service;

public class LocalizacaoService {

    public double calcularDistancia(double latitudeOrigem, double longitudeOrigem,
                                    double latitudeDestino, double longitudeDestino) {

        return Math.sqrt(
                Math.pow(latitudeDestino - latitudeOrigem, 2) +
                Math.pow(longitudeDestino - longitudeOrigem, 2)
        );
    }

    public String definirRegiao(double distancia) {
        if (distancia <= 5) {
            return "CENTRO";
        } else if (distancia <= 10) {
            return "BAIRRO";
        } else {
            return "EXPANDIDA";
        }
    }
}