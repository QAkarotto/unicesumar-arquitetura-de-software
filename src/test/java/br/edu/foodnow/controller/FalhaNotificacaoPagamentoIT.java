
package br.edu.foodnow.controller;

import br.edu.foodnow.model.Notificacao;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(notificacaoService.prepararNotificacao(
                any(Pedido.class), eq(StatusPagamento.APROVADO)))
                .thenReturn(new Notificacao(
                        "teste@email.com",
                        "Pagamento aprovado",
                        "Notificação de teste"));

        doThrow(new RuntimeException("Falha simulada no envio de e-mail"))
                .when(notificacaoService)
                .enviarNotificacao(any(Notificacao.class));

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

        verify(notificacaoService, timeout(5000).times(3))
                .enviarNotificacao(any(Notificacao.class));

        verify(notificacaoService, org.mockito.Mockito.times(1))
                .prepararNotificacao(any(Pedido.class), eq(StatusPagamento.APROVADO));
    }

    @Test
    void deveTentarNovamenteQuandoNotificacaoFalharTemporariamente() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);

        given().post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        when(notificacaoService.prepararNotificacao(
                any(Pedido.class), eq(StatusPagamento.APROVADO)))
                .thenReturn(new Notificacao(
                        "teste@email.com",
                        "Pagamento aprovado",
                        "Notificação de teste"));

        doThrow(new RuntimeException("Falha temporária no envio de e-mail"))
                .doNothing()
                .when(notificacaoService)
                .enviarNotificacao(any(Notificacao.class));

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

        verify(notificacaoService, timeout(5000).times(2))
                .enviarNotificacao(any(Notificacao.class));

        verify(notificacaoService, org.mockito.Mockito.times(1))
                .prepararNotificacao(any(Pedido.class), eq(StatusPagamento.APROVADO));
    }
}
