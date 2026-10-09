package main.java.foodnow.service;

public class PedidoService {

    public void criarPedido(int pedidoId, int clienteId) {
        System.out.println("Pedido " + pedidoId + " criado para o cliente " + clienteId);
    }

    public void confirmarPedido(int pedidoId) {
        System.out.println("Pedido " + pedidoId + " confirmado.");
    }

    public void cancelarPedido(int pedidoId) {
        System.out.println("Pedido " + pedidoId + " cancelado.");
    }

    public double calcularValorTotal(double subtotal, double taxaEntrega) {
        return subtotal + taxaEntrega;
    }

    public double calcularTaxaEntrega(double distancia) {
        return distancia * 2.50;
    }

    public int estimarTempoEntrega(double distancia) {
        return (int) (distancia * 5);
    }

    public boolean verificarAreaEntrega(double distancia) {
        return distancia <= 15;
    }

    public String consultarStatusPedido(int pedidoId) {
        return "EM PREPARO";
    }
}