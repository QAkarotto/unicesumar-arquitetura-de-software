
package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class EntregaEventoIT extends ApiTestSupport {

    @MockitoBean
    private FakeEmailClient fakeEmailClient;

    @Test
    void falhaNaNotificacaoNaoDeveDesfazerInicioDaEntrega() {

        // Arrange: cria e prepara um pedido válido.
        Cenario cenario = criarCenario(
                true, 25, -25.095, -50.160
        );

        long pedidoId = criarPedido(cenario);

        given()
                .post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "pedidoId", pedidoId,
                        "forma", "PIX",
                        "token", "TOKEN_APROVADO"
                ))
                .post("/pagamentos")
                .then()
                .statusCode(201);

        long entregaId = given()
                .get("/entregas/pedido/{pedidoId}", pedidoId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("id");

        // Simula uma falha no envio da notificação.
        doThrow(new IllegalStateException(
                "Serviço de e-mail indisponível"
        )).when(fakeEmailClient).enviar(
                eq("cliente@foodnow.test"),
                eq("Entrega iniciada"),
                anyString()
        );

        // Act: inicia a entrega.
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("status", "EM_ROTA"))
                .patch("/entregas/{id}/status", entregaId)
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_ROTA"));

        // Aguarda e verifica a execução do consumidor assíncrono.
        verify(fakeEmailClient, timeout(5000)).enviar(
                eq("cliente@foodnow.test"),
                eq("Entrega iniciada"),
                anyString()
        );

        // Assert: a entrega continua iniciada, apesar da falha no e-mail.
        given()
                .get("/entregas/{id}", entregaId)
                .then()
                .statusCode(200)
                .body("status", equalTo("EM_ROTA"));
    }
}
