package main.java.foodnow.service;

public class PagamentoService {

    public boolean processarPagamento(int pedidoId, double valor) {
        if (valor <= 0) {
            System.out.println("Pagamento inválido.");
            return false;
        }

        System.out.println("Pagamento do pedido " + pedidoId + " processado com sucesso.");
        return true;
    }

    public void confirmarPagamento(int pedidoId) {
        System.out.println("Pagamento do pedido " + pedidoId + " confirmado.");
    }

    public void cancelarPagamento(int pedidoId) {
        System.out.println("Pagamento do pedido " + pedidoId + " cancelado.");
    }

    public String consultarStatusPagamento(int pedidoId) {
        return "APROVADO";
    }

    public void emitirComprovante(int pedidoId) {
        System.out.println("Comprovante emitido para o pedido " + pedidoId);
    }
}