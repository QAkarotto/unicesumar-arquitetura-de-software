package foodnow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class ConfirmacaoPedidoEventoTest {

    @Autowired
    private PedidoService pedidoService;

    @MockitoBean
    private NotificacaoService notificacaoService;

    @Test
    void consumidorDeveSerExecutadoAposConfirmarPedido() {
        pedidoService.confirmarPedido(1);

        verify(notificacaoService, timeout(2000)).enviarConfirmacaoPedido(1);
    }

    @Test
    void consumidorDeveExecutarEmOutraThread() throws InterruptedException {
        AtomicReference<String> threadConsumidor = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocacao -> {
            threadConsumidor.set(Thread.currentThread().getName());
            latch.countDown();
            return null;
        }).when(notificacaoService).enviarConfirmacaoPedido(anyInt());

        pedidoService.confirmarPedido(3);

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertNotEquals(Thread.currentThread().getName(), threadConsumidor.get());
    }

    @Test
    void falhaNoConsumidorNaoDeveDesfazerPedidoConfirmado() {
        doThrow(new RuntimeException("Servico de notificacao indisponivel"))
                .when(notificacaoService).enviarConfirmacaoPedido(2);

        assertDoesNotThrow(() -> pedidoService.confirmarPedido(2));

        verify(notificacaoService, timeout(2000)).enviarConfirmacaoPedido(2);
    }
}
