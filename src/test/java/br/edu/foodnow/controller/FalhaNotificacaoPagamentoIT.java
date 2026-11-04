
package br.edu.foodnow.controller;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.service.NotificacaoService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class FalhaNotificacaoPagamentoIT extends ApiTestSupport {

    @MockitoBean
    private NotificacaoService notificacaoService;

    @Test
    void deveManterPagamentoAprovadoMesmoQuandoNotificacaoFalhar() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = criarPedido(cenario);

        given().post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        doThrow(new RuntimeException("Falha simulada no envio de e-mail"))
                .when(notificacaoService)
                .notificarPagamento(any(Pedido.class), eq(StatusPagamento.APROVADO));

        long pagamentoId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "pedidoId", pedidoId,
                        "forma", "PIX",
                        "token", "TOKEN_APROVADO"))
                .post("/pagamentos")
                .then()
                .statusCode(201)
                .body("status", equalTo("APROVADO"))
                .extract()
                .jsonPath()
                .getLong("id");

        given().get("/pagamentos/{id}", pagamentoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("APROVADO"));

        given().get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PAGO"));

        verify(notificacaoService, timeout(5000))
                .notificarPagamento(any(Pedido.class), eq(StatusPagamento.APROVADO));
    }
}
