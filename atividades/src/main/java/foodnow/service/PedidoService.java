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

    public String consultarStatusPedido(int pedidoId) {
        return "EM PREPARO";
    }
}