package br.edu.foodnow.controller;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.service.NotificacaoService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class PedidoConfirmadoEventoIT extends ApiTestSupport {

    @MockitoSpyBean
    private NotificacaoService notificacaoService;

    @Test
    void deveNotificarConfirmacaoEmOutraThreadDepoisDoCommit() throws Exception {
        CountDownLatch processado = new CountDownLatch(1);
        AtomicReference<String> thread = new AtomicReference<>();
        AtomicReference<Long> pedidoNotificado = new AtomicReference<>();
        doAnswer(invocacao -> {
            thread.set(Thread.currentThread().getName());
            pedidoNotificado.set(invocacao.<Pedido>getArgument(0).getId());
            invocacao.callRealMethod();
            processado.countDown();
            return null;
        }).when(notificacaoService).notificarConfirmacao(any());

        long pedidoId = criarPedido(criarCenario(true, 25, -25.095, -50.160));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        assertThat(processado.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(thread.get()).startsWith("foodnow-eventos-");
        assertThat(pedidoNotificado.get()).isEqualTo(pedidoId);
        assertThat(emailClient.getEnviadas()).extracting("assunto").containsExactly("Pedido confirmado");
    }

    @Test
    void deveManterPedidoConfirmadoQuandoANotificacaoFalha() throws Exception {
        CountDownLatch falhou = new CountDownLatch(1);
        doAnswer(invocacao -> {
            falhou.countDown();
            throw new IllegalStateException("servidor de e-mail fora do ar");
        }).when(notificacaoService).notificarConfirmacao(any());

        long pedidoId = criarPedido(criarCenario(true, 25, -25.095, -50.160));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        assertThat(falhou.await(5, TimeUnit.SECONDS)).isTrue();
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));
        given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_APROVADO"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));
        assertThat(emailClient.getEnviadas()).extracting("assunto").doesNotContain("Pedido confirmado");
    }
}
