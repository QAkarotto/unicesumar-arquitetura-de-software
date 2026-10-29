package foodnow.service;

public class PagamentoService {

    private final PaymentGateway paymentGateway;

    private static final int MAX_TENTATIVAS = 3;

    public PagamentoService() {
        this.paymentGateway = new FakePaymentGateway();
    }

    public PagamentoService(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    public boolean processarPagamento(int pedidoId, double valor) {

        RuntimeException ultimaExcecao = null;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {

            try {
                return paymentGateway.processarPagamento(pedidoId, valor);
            } catch (RuntimeException e) {
                ultimaExcecao = e;
            }

        }

        throw ultimaExcecao;
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