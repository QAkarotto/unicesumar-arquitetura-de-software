package main.java.foodnow.service;

public class EntregaService {

    public void criarEntrega(int pedidoId) {
        System.out.println("Entrega criada para o pedido " + pedidoId);
    }

    public void consultarEntrega(int pedidoId) {
        System.out.println("Consultando entrega do pedido " + pedidoId);
    }

    public double calcularDistancia(double origem, double destino) {
        return Math.abs(destino - origem);
    }

    public int calcularTempoEntrega(double distancia) {
        return (int) (distancia * 5);
    }

    public double calcularTaxaEntrega(double distancia) {
        return distancia * 2.5;
    }

    public void atualizarStatusEntrega(int pedidoId, String status) {
        System.out.println("Pedido " + pedidoId + " atualizado para: " + status);
    }
}