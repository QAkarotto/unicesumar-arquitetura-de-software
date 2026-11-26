package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Valida o evento PagamentoProcessado com o executor assíncrono real.
 */
@TestPropertySource(properties = "foodnow.eventos.assincrono=true")
class EventoPagamentoAssincronoIT extends ApiTestSupport {

    @MockitoSpyBean
    private FakeEmailClient emailEspiao;

    @Test
    void deveEnviarEmailDePagamentoEmOutraThreadAposOCommit() {
        long pedidoId = pedidoConfirmado();
        AtomicReference<String> threadDoEnvio = new AtomicReference<>();
        doAnswer(invocacao -> {
            threadDoEnvio.set(Thread.currentThread().getName());
            return invocacao.callRealMethod();
        }).when(emailEspiao).enviar(argThat((Notificacao n) -> n.getAssunto().startsWith("Pagamento")));

        given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));

        verify(emailEspiao, timeout(5000)).enviar(argThat((Notificacao n) -> "Pagamento aprovado".equals(n.getAssunto())));
        assertThat(threadDoEnvio.get()).startsWith("eventos-");
        assertThat(emailEspiao.getEnviadas()).extracting("assunto").contains("Pagamento aprovado");
    }

    @Test
    void falhaNoEnvioDoEmailNaoDeveDesfazerOPagamento() {
        long pedidoId = pedidoConfirmado();
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(emailEspiao).enviar(any(Notificacao.class));

        given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));

        verify(emailEspiao, timeout(5000)).enviar(any(Notificacao.class));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200).body("status", equalTo("PAGO"));
        given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(200)
                .body("pedidoId", equalTo((int) pedidoId));
        assertThat(emailEspiao.getEnviadas()).extracting("assunto").doesNotContain("Pagamento aprovado");
    }

    private long pedidoConfirmado() {
        long pedidoId = criarPedido(criarCenario(true, 25, -25.095, -50.160));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);
        return pedidoId;
    }

    private Map<String, Object> pagamento(long pedidoId) {
        return Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_OK");
    }
}
