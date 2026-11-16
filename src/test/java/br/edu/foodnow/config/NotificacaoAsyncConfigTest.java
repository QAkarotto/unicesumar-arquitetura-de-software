package br.edu.foodnow.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(OutputCaptureExtension.class)
class NotificacaoAsyncConfigTest {
    @Test
    void filaCheiaDeveRegistrarDescarteSemExecutarNaThreadProdutora(CapturedOutput saida) throws Exception {
        var executor = new NotificacaoAsyncConfig().notificacoesExecutor();
        executor.initialize();
        CountDownLatch iniciou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch concluidas = new CountDownLatch(100);
        AtomicBoolean rejeitadaExecutou = new AtomicBoolean();
        try {
            executor.execute(() -> {
                iniciou.countDown();
                try {
                    if (!liberar.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Teste não liberou executor");
                    }
                } catch (InterruptedException excecao) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(excecao);
                }
            });
            assertThat(iniciou.await(10, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < 100; i++) {
                executor.execute(concluidas::countDown);
            }

            assertThatCode(() -> executor.execute(() -> rejeitadaExecutou.set(true)))
                    .doesNotThrowAnyException();
            assertThat(rejeitadaExecutou).isFalse();
            assertThat(saida.getAll()).contains("Notificação descartada: executor indisponível ou fila cheia.");

            liberar.countDown();
            assertThat(concluidas.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            liberar.countDown();
            executor.shutdown();
        }
    }
}
