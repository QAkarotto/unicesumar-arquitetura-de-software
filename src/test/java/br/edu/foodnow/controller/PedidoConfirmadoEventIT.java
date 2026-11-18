package br.edu.foodnow.controller;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.service.NotificacaoService;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

class PedidoConfirmadoEventIT extends ApiTestSupport {

    @MockitoBean
    private NotificacaoService notificacaoService;

    @Test
    void deveExecutarConsumidorEmOutraThreadAposConfirmacao()
            throws InterruptedException {

        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Long pedidoId = criarPedido(cenario);

        CountDownLatch consumidorExecutado = new CountDownLatch(1);
        AtomicReference<String> threadExecutora = new AtomicReference<>();

        doAnswer(invocacao -> {
            threadExecutora.set(Thread.currentThread().getName());
            consumidorExecutado.countDown();
            return null;
        }).when(notificacaoService).notificarConfirmacao(any(Pedido.class));

        given()
                .post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        assertThat(consumidorExecutado.await(5, TimeUnit.SECONDS))
                .isTrue();

        assertThat(threadExecutora.get())
                .startsWith("foodnow-async-");

        verify(notificacaoService).notificarConfirmacao(
                argThat(pedido -> pedidoId.equals(pedido.getId()))
        );
    }

    @Test
    void falhaNaNotificacaoNaoDeveDesfazerConfirmacaoDoPedido()
            throws InterruptedException {

        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);

        CountDownLatch consumidorExecutado = new CountDownLatch(1);

        doAnswer(invocacao -> {
            consumidorExecutado.countDown();

            throw new IllegalStateException(
                    "Falha simulada no envio do e-mail"
            );

        }).when(notificacaoService).notificarConfirmacao(any(Pedido.class));

        given()
                .post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        assertThat(consumidorExecutado.await(5, TimeUnit.SECONDS))
                .isTrue();

        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMADO"));
    }
}