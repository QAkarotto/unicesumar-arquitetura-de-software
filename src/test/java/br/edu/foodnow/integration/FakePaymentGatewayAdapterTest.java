package br.edu.foodnow.integration;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FakePaymentGatewayAdapterTest {

    @Test
    void deveTraduzirSolicitacaoParaFormatoDoProvedorEAprovar() {
        GatewayQueRegistra gateway = new GatewayQueRegistra();
        FakePaymentGatewayAdapter adapter = new FakePaymentGatewayAdapter(gateway);

        ResultadoPagamento resultado = adapter.autorizar(solicitacao(7L, "TOKEN_OK"));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(resultado.codigoTransacao()).isEqualTo("FN-7");
        assertThat(resultado.mensagem()).isEqualTo("Pagamento autorizado");
        assertThat(gateway.requisicoes).singleElement().satisfies(requisicao -> {
            assertThat(requisicao.orderReference()).isEqualTo("7");
            assertThat(requisicao.amountInCents()).isEqualTo(6655);
            assertThat(requisicao.paymentToken()).isEqualTo("TOKEN_OK");
            assertThat(requisicao.paymentMethod()).isEqualTo("PIX");
            assertThat(requisicao.deliveryZone()).isEqualTo("CENTRAL");
            assertThat(requisicao.originCoordinates()).isEqualTo("-25.100000,-50.150000");
            assertThat(requisicao.destinationCoordinates()).isEqualTo("-25.095000,-50.160000");
        });
    }

    @Test
    void deveTraduzirRecusaDoProvedorParaPagamentoRejeitado() {
        FakePaymentGatewayAdapter adapter = new FakePaymentGatewayAdapter(new FakePaymentGateway());

        ResultadoPagamento resultado = adapter.autorizar(solicitacao(8L, "REJEITADO"));

        assertThat(resultado.status()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(resultado.codigoTransacao()).isEqualTo("FN-8");
        assertThat(resultado.mensagem()).isEqualTo("Pagamento rejeitado pelo provedor simulado");
    }

    private SolicitacaoPagamento solicitacao(Long pedidoId, String token) {
        return new SolicitacaoPagamento(pedidoId, new BigDecimal("66.55"), token, FormaPagamento.PIX, "CENTRAL",
                new Localizacao(-25.100, -50.150), new Localizacao(-25.095, -50.160));
    }

    private static final class GatewayQueRegistra extends FakePaymentGateway {
        private final List<GatewayRequest> requisicoes = new ArrayList<>();

        @Override
        public GatewayResult processarPagamento(GatewayRequest request) {
            requisicoes.add(request);
            return super.processarPagamento(request);
        }
    }
}
