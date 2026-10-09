package main.java.foodnow.service;

public class NotificacaoService {

    public void enviarConfirmacaoPedido(int pedidoId) {
        System.out.println("Pedido " + pedidoId + " confirmado.");
    }

    public void enviarConfirmacaoPagamento(int pedidoId) {
        System.out.println("Pagamento do pedido " + pedidoId + " confirmado.");
    }

    public void enviarAtualizacaoEntrega(int pedidoId, String status) {
        System.out.println("Entrega do pedido " + pedidoId + " está com status: " + status);
    }

    public void enviarMensagemCliente(int clienteId, String mensagem) {
        System.out.println("Mensagem para o cliente " + clienteId + ": " + mensagem);
    }

    public void enviarNotificacao(String destinatario, String mensagem) {
        System.out.println("Notificação enviada para " + destinatario + ": " + mensagem);
    }
}