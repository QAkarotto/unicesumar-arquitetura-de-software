package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class FoodNowPedidoObservacaoIT extends ApiTestSupport {

    @Test
    void devePreservarObservacaoDuranteOperacoesDoPedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "Não incluir talheres";

        Map<String, Object> requisicao = requisicaoPedido(cenario);
        requisicao.put("observacao", observacao);

        long pedidoId = given().contentType(ContentType.JSON).body(requisicao)
                .post("/pedidos").then().statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract().jsonPath().getLong("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));

        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveContinuarAceitandoPedidoSemObservacao() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = given().contentType(ContentType.JSON).body(requisicaoPedido(cenario))
                .post("/pedidos").then().statusCode(201)
                .extract().jsonPath().getLong("id");

        Map<?, ?> resposta = given().get("/pedidos/{id}", pedidoId)
                .then().statusCode(200).extract().as(Map.class);

        assertThat(resposta.containsKey("observacao")).isFalse();
    }

    @Test
    void deveTratarObservacaoNullComoAusente() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> requisicao = requisicaoPedido(cenario);
        requisicao.put("observacao", null);

        Map<?, ?> resposta = given().contentType(ContentType.JSON).body(requisicao)
                .post("/pedidos").then().statusCode(201).extract().as(Map.class);

        assertThat(resposta.containsKey("observacao")).isFalse();
    }

    @Test
    void deveAceitarObservacaoVazia() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> requisicao = requisicaoPedido(cenario);
        requisicao.put("observacao", "");

        given().contentType(ContentType.JSON).body(requisicao)
                .post("/pedidos").then().statusCode(201)
                .body("observacao", equalTo(""));
    }

    @Test
    void deveAceitarObservacaoCom500Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "a".repeat(500);
        Map<String, Object> requisicao = requisicaoPedido(cenario);
        requisicao.put("observacao", observacao);

        long pedidoId = given().contentType(ContentType.JSON).body(requisicao)
                .post("/pedidos").then().statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract().jsonPath().getLong("id");

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveRejeitarObservacaoCom501Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> requisicao = requisicaoPedido(cenario);
        requisicao.put("observacao", "a".repeat(501));

        given().contentType(ContentType.JSON).body(requisicao)
                .post("/pedidos").then().statusCode(400)
                .body("erro", equalTo("VALIDATION_ERROR"));
    }

    private Map<String, Object> requisicaoPedido(Cenario cenario) {
        Map<String, Object> requisicao = new HashMap<>();
        requisicao.put("clienteId", cenario.clienteId());
        requisicao.put("restauranteId", cenario.restauranteId());
        requisicao.put("enderecoEntregaId", cenario.enderecoId());
        requisicao.put("itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 1)));
        return requisicao;
    }
}