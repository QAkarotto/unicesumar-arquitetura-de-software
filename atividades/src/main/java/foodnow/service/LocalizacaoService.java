package main.java.foodnow.service;

public class LocalizacaoService {

    public double calcularDistancia(double latitudeOrigem, double longitudeOrigem,
                                    double latitudeDestino, double longitudeDestino) {

        double distancia = Math.sqrt(
                Math.pow(latitudeDestino - latitudeOrigem, 2) +
                Math.pow(longitudeDestino - longitudeOrigem, 2)
        );

        return distancia;
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

    public boolean verificarAreaEntrega(double distancia) {
        return distancia <= 15;
    }

    public int calcularTempoEntrega(double distancia) {
        return (int) (distancia * 5);
    }

    public double calcularTaxaEntrega(double distancia) {
        return distancia * 2.50;
    }
}