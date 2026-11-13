package br.edu.foodnow.integration;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayPagamentoAdapterTest {
    private final GatewayPagamentoAdapter adapter =
            new GatewayPagamentoAdapter(new FakePaymentGateway());

    @Test
    void deveTraduzirPagamentoAutorizadoParaContratoDaAplicacao() {
        var resultado = adapter.processar(solicitacao("TOKEN-VALIDO"));

        assertEquals(StatusPagamento.APROVADO, resultado.status());
        assertEquals("FN-10", resultado.codigoExterno());
        assertEquals("Pagamento autorizado", resultado.mensagemProvedor());
    }

    @Test
    void deveTraduzirPagamentoRecusadoParaContratoDaAplicacao() {
        var resultado = adapter.processar(solicitacao("REJEITADO"));

        assertEquals(StatusPagamento.REJEITADO, resultado.status());
        assertEquals("FN-10", resultado.codigoExterno());
        assertEquals("Pagamento rejeitado pelo provedor simulado", resultado.mensagemProvedor());
    }

    private SolicitacaoPagamento solicitacao(String token) {
        return new SolicitacaoPagamento(10L, new BigDecimal("42.30"), token,
                FormaPagamento.CARTAO_CREDITO, "CENTRAL", "-25.0,-50.0", "-25.1,-50.1");
    }
}
