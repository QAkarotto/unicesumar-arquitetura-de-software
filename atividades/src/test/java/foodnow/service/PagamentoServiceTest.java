package foodnow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PagamentoServiceTest {

    static class GatewayTeste implements PaymentGateway {

        @Override
        public boolean processarPagamento(int pedidoId, double valor) {
            return valor > 0;
        }
    }

    static class GatewayRetry implements PaymentGateway {

        int chamadas = 0;

        @Override
        public boolean processarPagamento(int pedidoId, double valor) {

            chamadas++;

            if (chamadas == 1) {
                throw new RuntimeException("Falha temporária");
            }

            return true;
        }
    }

    static class GatewaySempreFalha implements PaymentGateway {

        int chamadas = 0;

        @Override
        public boolean processarPagamento(int pedidoId, double valor) {

            chamadas++;
            throw new RuntimeException("Gateway indisponível");
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

    @Test
    void deveProcessarPagamentoAposRetry() {

        GatewayRetry gateway = new GatewayRetry();

        PagamentoService pagamentoService = new PagamentoService(gateway);

        assertTrue(pagamentoService.processarPagamento(1, 100));

        assertEquals(2, gateway.chamadas);
    }

    @Test
    void deveFalharAposTresTentativas() {

        GatewaySempreFalha gateway = new GatewaySempreFalha();

        PagamentoService pagamentoService = new PagamentoService(gateway);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> pagamentoService.processarPagamento(1, 100)
        );

        assertEquals("Gateway indisponível", exception.getMessage());
        assertEquals(3, gateway.chamadas);
    }
}