package foodnow.service;

public class FakePaymentGateway implements PaymentGateway {

    @Override
    public boolean processarPagamento(int pedidoId, double valor) {

        if (valor <= 0) {
            System.out.println("Pagamento inválido.");
            return false;
        }

        System.out.println("Pagamento do pedido " + pedidoId + " processado com sucesso.");
        return true;
    }
}