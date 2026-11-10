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
    private final GatewayEspiao gateway = new GatewayEspiao();
    private final GatewayPagamentoAdapter adapter = new GatewayPagamentoAdapter(gateway);

    @Test
    void deveTraduzirSolicitacaoParaOFormatoDoGateway() {
        adapter.processar(solicitacao("TOKEN_OK"));

        var enviada = gateway.ultimaRequisicao;
        assertThat(enviada.orderReference()).isEqualTo("10");
        assertThat(enviada.amountInCents()).isEqualTo(4230);
        assertThat(enviada.paymentToken()).isEqualTo("TOKEN_OK");
        assertThat(enviada.paymentMethod()).isEqualTo("PIX");
        assertThat(enviada.deliveryZone()).isEqualTo("CENTRAL");
        assertThat(enviada.originCoordinates()).isEqualTo("-25.100000,-50.150000");
        assertThat(enviada.destinationCoordinates()).isEqualTo("-25.095000,-50.160000");
    }

    @Test
    void deveConverterPagamentoAutorizadoEmAprovado() {
        ResultadoPagamento resultado = adapter.processar(solicitacao("TOKEN_OK"));

        assertThat(resultado).isEqualTo(new ResultadoPagamento(StatusPagamento.APROVADO, "FN-10",
                "Pagamento autorizado"));
    }

    @Test
    void deveConverterPagamentoRecusadoEmRejeitado() {
        ResultadoPagamento resultado = adapter.processar(solicitacao("REJEITADO"));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(resultado.codigoTransacao()).isEqualTo("FN-10");
        assertThat(resultado.mensagem()).isEqualTo("Pagamento rejeitado pelo provedor simulado");
    }

    private SolicitacaoPagamento solicitacao(String token) {
        return new SolicitacaoPagamento(10L, new BigDecimal("42.30"), FormaPagamento.PIX, token, "CENTRAL",
                new Localizacao(-25.100, -50.150), new Localizacao(-25.095, -50.160));
    }

    private static class GatewayEspiao extends FakePaymentGateway {
        private GatewayRequest ultimaRequisicao;

        @Override
        public GatewayResult processarPagamento(GatewayRequest request) {
            ultimaRequisicao = request;
            return super.processarPagamento(request);
        }
    }
}
