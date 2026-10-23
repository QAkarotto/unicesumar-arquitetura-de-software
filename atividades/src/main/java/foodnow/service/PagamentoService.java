package foodnow.service;

public class PagamentoService {

    private final PaymentGateway paymentGateway;

    public PagamentoService() {
        this.paymentGateway = new FakePaymentGateway();
    }

    public PagamentoService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public boolean processarPagamento(int pedidoId, double valor) {
        return paymentGateway.processarPagamento(pedidoId, valor);
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