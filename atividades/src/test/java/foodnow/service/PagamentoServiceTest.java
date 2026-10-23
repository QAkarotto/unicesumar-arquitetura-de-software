package foodnow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PagamentoServiceTest {

    static class GatewayTeste implements PaymentGateway {

        @Override
        public boolean processarPagamento(int pedidoId, double valor) {
            return valor > 0;
        }
    }

    @Test
    void deveProcessarPagamentoValido() {

        PaymentGateway gateway = new GatewayTeste();
        PagamentoService pagamentoService = new PagamentoService(gateway);

        assertTrue(pagamentoService.processarPagamento(1, 100));
    }

    @Test
    void deveRejeitarPagamentoInvalido() {

        PaymentGateway gateway = new GatewayTeste();
        PagamentoService pagamentoService = new PagamentoService(gateway);

        assertFalse(pagamentoService.processarPagamento(1, 0));
    }
}