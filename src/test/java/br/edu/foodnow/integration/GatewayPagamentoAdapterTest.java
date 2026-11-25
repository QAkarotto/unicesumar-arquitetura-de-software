package br.edu.foodnow.integration;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayPagamentoAdapterTest {
    private final GatewayCapturado gateway = new GatewayCapturado();
    private final GatewayPagamentoAdapter adapter = new GatewayPagamentoAdapter(gateway);

    @Test
    void deveTraduzirSolicitacaoParaOFormatoDoProvedor() {
        adapter.processar(solicitacao("TOKEN_OK"));

        assertThat(gateway.ultima).isEqualTo(new FakePaymentGateway.GatewayRequest("10", 4230, "TOKEN_OK",
                "PIX", "CENTRAL", "-25.100000,-50.150000", "-25.095000,-50.160000"));
    }

    @Test
    void deveTraduzirAutorizacaoDoProvedorEmPagamentoAprovado() {
        ResultadoPagamento resultado = adapter.processar(solicitacao("TOKEN_OK"));

        assertThat(resultado).isEqualTo(new ResultadoPagamento(StatusPagamento.APROVADO, "FN-10",
                "Pagamento autorizado"));
    }

    @Test
    void deveTraduzirRecusaDoProvedorEmPagamentoRejeitado() {
        ResultadoPagamento resultado = adapter.processar(solicitacao("REJEITADO"));

        assertThat(resultado).isEqualTo(new ResultadoPagamento(StatusPagamento.REJEITADO, "FN-10",
                "Pagamento rejeitado pelo provedor simulado"));
    }

    private SolicitacaoPagamento solicitacao(String token) {
        return new SolicitacaoPagamento(10L, new BigDecimal("42.30"), FormaPagamento.PIX, token, "CENTRAL",
                new Localizacao(-25.100, -50.150), new Localizacao(-25.095, -50.160));
    }

    private static class GatewayCapturado extends FakePaymentGateway {
        private GatewayRequest ultima;

        @Override
        public GatewayResult processarPagamento(GatewayRequest request) {
            ultima = request;
            return super.processarPagamento(request);
        }
    }
}
