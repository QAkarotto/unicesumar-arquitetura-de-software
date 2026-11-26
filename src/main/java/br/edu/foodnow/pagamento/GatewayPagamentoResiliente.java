package br.edu.foodnow.pagamento;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Decorator do contrato {@link GatewayPagamento} que aplica timeout por tentativa e retry com espera
 * crescente para falhas transitórias. O retry é seguro porque todas as tentativas usam a mesma
 * chave de idempotência da solicitação, impedindo cobrança duplicada no provedor.
 */
public class GatewayPagamentoResiliente implements GatewayPagamento, AutoCloseable {
    private final GatewayPagamento delegado;
    private final Duration timeout;
    private final int maximoTentativas;
    private final Duration esperaInicial;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public GatewayPagamentoResiliente(GatewayPagamento delegado, Duration timeout, int maximoTentativas,
                                      Duration esperaInicial) {
        if (maximoTentativas < 1) {
            throw new IllegalArgumentException("Deve haver ao menos uma tentativa");
        }
        this.delegado = delegado;
        this.timeout = timeout;
        this.maximoTentativas = maximoTentativas;
        this.esperaInicial = esperaInicial;
    }

    @Override
    public ResultadoPagamento autorizar(SolicitacaoPagamento solicitacao) {
        RuntimeException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= maximoTentativas; tentativa++) {
            if (tentativa > 1) {
                aguardar(esperaInicial.multipliedBy(1L << (tentativa - 2)));
            }
            try {
                return tentar(solicitacao);
            } catch (GatewayPagamentoIndisponivelException falha) {
                ultimaFalha = falha;
            }
        }
        throw new GatewayPagamentoIndisponivelException(
                "Provedor de pagamento indisponível após " + maximoTentativas + " tentativas", ultimaFalha);
    }

    private ResultadoPagamento tentar(SolicitacaoPagamento solicitacao) {
        Future<ResultadoPagamento> chamada = executor.submit(() -> delegado.autorizar(solicitacao));
        try {
            return chamada.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException excecao) {
            chamada.cancel(true);
            throw new GatewayPagamentoIndisponivelException(
                    "Provedor de pagamento não respondeu em " + timeout.toMillis() + " ms", excecao);
        } catch (ExecutionException excecao) {
            if (excecao.getCause() instanceof RuntimeException causa) {
                throw causa;
            }
            throw new IllegalStateException(excecao.getCause());
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            chamada.cancel(true);
            throw new GatewayPagamentoIndisponivelException("Chamada ao provedor de pagamento interrompida",
                    excecao);
        }
    }

    private void aguardar(Duration espera) {
        try {
            Thread.sleep(espera.toMillis());
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new GatewayPagamentoIndisponivelException("Retry do pagamento interrompido", excecao);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
