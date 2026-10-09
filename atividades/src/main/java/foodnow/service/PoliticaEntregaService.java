package main.java.foodnow.service;

public class PoliticaEntregaService {

    public double calcularDistancia(double origem, double destino) {
        return Math.abs(destino - origem);
    }

    public double calcularTaxaEntrega(double distancia) {
        return distancia * 2.50;
    }

    public int calcularTempoEntrega(double distancia) {
        return (int) (distancia * 5);
    }

    public boolean verificarAreaEntrega(double distancia) {
        return distancia <= 15;
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

    public void exibirResumoEntrega(double origem, double destino) {
        double distancia = calcularDistancia(origem, destino);

        System.out.println("Distância: " + distancia + " km");
        System.out.println("Taxa: R$ " + calcularTaxaEntrega(distancia));
        System.out.println("Tempo: " + calcularTempoEntrega(distancia) + " minutos");
        System.out.println("Região: " + definirRegiao(distancia));
        System.out.println("Área atendida: " + verificarAreaEntrega(distancia));
    }
}