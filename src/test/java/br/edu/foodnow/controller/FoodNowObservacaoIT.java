package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

class FoodNowObservacaoIT extends ApiTestSupport {

    private Map<String, Object> pedidoRequest(
            Cenario cenario, String observacao) {
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("clienteId", cenario.clienteId());
        pedido.put("restauranteId", cenario.restauranteId());
        pedido.put("enderecoEntregaId", cenario.enderecoId());
        pedido.put("itens", List.of(
                Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
        pedido.put("observacao", observacao);
        return pedido;
    }

    @Test
    void deveManterObservacaoNasOperacoesDoPedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "Não incluir talheres";

        long pedidoId = given()
                .contentType(ContentType.JSON)
                .body(pedidoRequest(cenario, observacao))
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract().jsonPath().getLong("id");

        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(),
                        "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));

        given()
                .post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));

        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveAceitarObservacaoCom500Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "a".repeat(500);

        given()
                .contentType(ContentType.JSON)
                .body(pedidoRequest(cenario, observacao))
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveRejeitarObservacaoCom501Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "a".repeat(501);

        given()
                .contentType(ContentType.JSON)
                .body(pedidoRequest(cenario, observacao))
                .post("/pedidos")
                .then()
                .statusCode(400)
                .body("erro", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void deveAceitarPedidoSemObservacao() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> pedido = pedidoRequest(cenario, null);

        given()
                .contentType(ContentType.JSON)
                .body(pedido)
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", org.hamcrest.Matchers.nullValue()); //wawa modificado
    }

    @Test
    void deveNormalizarObservacaoVaziaParaAusencia() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        given()
                .contentType(ContentType.JSON)
                .body(pedidoRequest(cenario, ""))
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", org.hamcrest.Matchers.nullValue());//wawa modificado
    }
}
