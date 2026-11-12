package foodnow.service;

public interface PaymentGateway {

    boolean processarPagamento(int pedidoId, double valor);

}