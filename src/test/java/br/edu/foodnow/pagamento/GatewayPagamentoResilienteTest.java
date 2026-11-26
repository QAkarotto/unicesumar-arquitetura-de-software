package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.StatusPagamento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayPagamentoResilienteTest {
    private static final ResultadoPagamento APROVADO =
            new ResultadoPagamento(StatusPagamento.APROVADO, "TX-1", "Pagamento autorizado");
    private static final ResultadoPagamento REJEITADO =
            new ResultadoPagamento(StatusPagamento.REJEITADO, "TX-1", "Pagamento recusado");

    private GatewayPagamentoResiliente resiliente;

    @AfterEach
    void encerrar() {
        if (resiliente != null) {
            resiliente.close();
        }
    }

    @Test
    void deveAutorizarNaPrimeiraTentativaSemRetry() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> APROVADO);
        resiliente = criar(gateway, Duration.ofMillis(500), 3);

        assertThat(resiliente.autorizar(solicitacao())).isEqualTo(APROVADO);
        assertThat(gateway.chamadas()).isEqualTo(1);
    }

    @Test
    void deveRepetirFalhasTransitoriasAteObterSucesso() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> {
            if (tentativa < 3) {
                throw indisponivel();
            }
            return APROVADO;
        });
        resiliente = criar(gateway, Duration.ofMillis(500), 3);

        assertThat(resiliente.autorizar(solicitacao())).isEqualTo(APROVADO);
        assertThat(gateway.chamadas()).isEqualTo(3);
    }

    @Test
    void deveUsarAMesmaSolicitacaoEChaveDeIdempotenciaEmTodasAsTentativas() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> {
            if (tentativa == 1) {
                throw indisponivel();
            }
            return APROVADO;
        });
        resiliente = criar(gateway, Duration.ofMillis(500), 3);

        resiliente.autorizar(solicitacao());

        assertThat(gateway.solicitacoes).hasSize(2)
                .extracting(SolicitacaoPagamento::chaveIdempotencia).containsOnly("pedido-42");
    }

    @Test
    void deveDesistirAposEsgotarAsTentativas() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> {
            throw indisponivel();
        });
        resiliente = criar(gateway, Duration.ofMillis(500), 3);

        assertThatThrownBy(() -> resiliente.autorizar(solicitacao()))
                .isInstanceOf(GatewayPagamentoIndisponivelException.class)
                .hasMessage("Provedor de pagamento indisponível após 3 tentativas")
                .hasCauseInstanceOf(GatewayPagamentoIndisponivelException.class);
        assertThat(gateway.chamadas()).isEqualTo(3);
    }

    @Test
    void deveTratarRespostaLentaComoFalhaTransitoriaERepetir() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> {
            if (tentativa == 1) {
                dormir(Duration.ofSeconds(2));
            }
            return APROVADO;
        });
        resiliente = criar(gateway, Duration.ofMillis(100), 2);

        long inicio = System.nanoTime();
        assertThat(resiliente.autorizar(solicitacao())).isEqualTo(APROVADO);

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(1));
        assertThat(gateway.chamadas()).isEqualTo(2);
    }

    @Test
    void deveFalharPorTimeoutQuandoProvedorNuncaResponde() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> {
            dormir(Duration.ofSeconds(2));
            return APROVADO;
        });
        resiliente = criar(gateway, Duration.ofMillis(50), 2);

        assertThatThrownBy(() -> resiliente.autorizar(solicitacao()))
                .isInstanceOf(GatewayPagamentoIndisponivelException.class)
                .cause().hasMessage("Provedor de pagamento não respondeu em 50 ms");
    }

    @Test
    void naoDeveRepetirPagamentoRecusadoPeloProvedor() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> REJEITADO);
        resiliente = criar(gateway, Duration.ofMillis(500), 3);

        assertThat(resiliente.autorizar(solicitacao()).status()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(gateway.chamadas()).isEqualTo(1);
    }

    @Test
    void naoDeveRepetirErrosQueNaoSaoTransitorios() {
        GatewayRoteirizado gateway = new GatewayRoteirizado(tentativa -> {
            throw new IllegalArgumentException("Solicitação inválida");
        });
        resiliente = criar(gateway, Duration.ofMillis(500), 3);

        assertThatThrownBy(() -> resiliente.autorizar(solicitacao()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Solicitação inválida");
        assertThat(gateway.chamadas()).isEqualTo(1);
    }

    @Test
    void deveExigirAoMenosUmaTentativa() {
        assertThatThrownBy(() -> new GatewayPagamentoResiliente(solicitacao -> APROVADO,
                Duration.ofMillis(100), 0, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private GatewayPagamentoResiliente criar(GatewayPagamento gateway, Duration timeout, int tentativas) {
        return new GatewayPagamentoResiliente(gateway, timeout, tentativas, Duration.ofMillis(1));
    }

    private SolicitacaoPagamento solicitacao() {
        return new SolicitacaoPagamento(42L, new BigDecimal("66.55"), "TOKEN", FormaPagamento.PIX, "CENTRAL",
                new Localizacao(-25.100, -50.150), new Localizacao(-25.095, -50.160));
    }

    private static GatewayPagamentoIndisponivelException indisponivel() {
        return new GatewayPagamentoIndisponivelException("Provedor fora do ar", null);
    }

    private static void dormir(Duration duracao) {
        try {
            Thread.sleep(duracao.toMillis());
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(excecao);
        }
    }

    /** Gateway de teste cujo comportamento depende do número da tentativa (1, 2, 3...). */
    private static final class GatewayRoteirizado implements GatewayPagamento {
        private final Function<Integer, ResultadoPagamento> roteiro;
        private final List<SolicitacaoPagamento> solicitacoes = new ArrayList<>();

        GatewayRoteirizado(Function<Integer, ResultadoPagamento> roteiro) {
            this.roteiro = roteiro;
        }

        @Override
        public synchronized ResultadoPagamento autorizar(SolicitacaoPagamento solicitacao) {
            solicitacoes.add(solicitacao);
            return roteiro.apply(solicitacoes.size());
        }

        int chamadas() {
            return solicitacoes.size();
        }
    }
}
