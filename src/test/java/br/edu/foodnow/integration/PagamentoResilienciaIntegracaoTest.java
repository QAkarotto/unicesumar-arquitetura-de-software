package br.edu.foodnow.integration;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.GatewayPagamentoIndisponivelException;
import br.edu.foodnow.pagamento.GatewayPagamentoResiliente;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida timeout, retry e idempotência com o adapter e o provedor simulado reais.
 */
class PagamentoResilienciaIntegracaoTest {

    @Test
    void provedorDeveSerIdempotentePorChave() {
        FakePaymentGateway gateway = new FakePaymentGateway();
        var requisicao = FakePaymentGateway.GatewayRequest.from(5L, BigDecimal.TEN, "TOKEN", "PIX",
                "CENTRAL", "0,0", "1,1", "pedido-5");

        var primeira = gateway.processarPagamento(requisicao);
        var repetida = gateway.processarPagamento(requisicao);
        gateway.processarPagamento(FakePaymentGateway.GatewayRequest.from(6L, BigDecimal.TEN, "TOKEN", "PIX",
                "CENTRAL", "0,0", "1,1", "pedido-6"));

        assertThat(repetida).isEqualTo(primeira);
        assertThat(gateway.getCobrancasProcessadas()).isEqualTo(2);
    }

    @Test
    void retryAposRespostaPerdidaNaoDeveCobrarDuasVezes() {
        ProvedorComRespostaPerdida provedor = new ProvedorComRespostaPerdida();
        try (var resiliente = new GatewayPagamentoResiliente(new FakePaymentGatewayAdapter(provedor),
                Duration.ofMillis(100), 3, Duration.ofMillis(1))) {

            ResultadoPagamento resultado = resiliente.autorizar(solicitacao(9L));

            assertThat(resultado.status()).isEqualTo(StatusPagamento.APROVADO);
            assertThat(resultado.codigoTransacao()).isEqualTo("FN-9");
            assertThat(provedor.getCobrancasProcessadas()).isEqualTo(1);
        }
    }

    @Test
    void adapterDeveConverterFalhaDoProvedorEmIndisponibilidade() {
        FakePaymentGateway foraDoAr = new FakePaymentGateway() {
            @Override
            public GatewayResult processarPagamento(GatewayRequest request) {
                throw new IllegalStateException("Connection refused");
            }
        };
        FakePaymentGatewayAdapter adapter = new FakePaymentGatewayAdapter(foraDoAr);

        assertThatThrownBy(() -> adapter.autorizar(solicitacao(10L)))
                .isInstanceOf(GatewayPagamentoIndisponivelException.class)
                .hasRootCauseMessage("Connection refused");
    }

    private SolicitacaoPagamento solicitacao(Long pedidoId) {
        return new SolicitacaoPagamento(pedidoId, new BigDecimal("66.55"), "TOKEN", FormaPagamento.PIX, "CENTRAL",
                new Localizacao(-25.100, -50.150), new Localizacao(-25.095, -50.160));
    }

    /** Na primeira chamada, registra a cobrança e demora a responder, simulando a perda da resposta. */
    private static final class ProvedorComRespostaPerdida extends FakePaymentGateway {
        private final AtomicBoolean primeiraChamada = new AtomicBoolean(true);

        @Override
        public GatewayResult processarPagamento(GatewayRequest request) {
            GatewayResult resultado = super.processarPagamento(request);
            if (primeiraChamada.getAndSet(false)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException excecao) {
                    Thread.currentThread().interrupt();
                }
            }
            return resultado;
        }
    }
}
