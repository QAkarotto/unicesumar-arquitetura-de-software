package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

class PedidoObservacaoIT extends ApiTestSupport {

    @Test
    void devePersistirEManterObservacaoNasOperacoesDoPedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "Não incluir talheres";

        long pedidoId = given().contentType(ContentType.JSON)
                .body(requisicaoPedido(cenario, observacao, true))
                .post("/pedidos").then().statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract().jsonPath().getLong("id");

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));
        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveTratarAusenciaNullVazioEEspacosComoAusencia() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        given().contentType(ContentType.JSON).body(requisicaoPedido(cenario, null, false))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")));
        given().contentType(ContentType.JSON).body(requisicaoPedido(cenario, null, true))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")));
        given().contentType(ContentType.JSON).body(requisicaoPedido(cenario, "", true))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")));
        given().contentType(ContentType.JSON).body(requisicaoPedido(cenario, "   ", true))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")));
    }

    @Test
    void deveAceitar500ERejeitar501Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        given().contentType(ContentType.JSON).body(requisicaoPedido(cenario, "a".repeat(500), true))
                .post("/pedidos").then().statusCode(201)
                .body("observacao", equalTo("a".repeat(500)));
        given().contentType(ContentType.JSON).body(requisicaoPedido(cenario, "a".repeat(501), true))
                .post("/pedidos").then().statusCode(400)
                .body("erro", equalTo("VALIDATION_ERROR"))
                .body("mensagem", containsString("observacao"))
                .body("mensagem", containsString("500"));
    }

    private Map<String, Object> requisicaoPedido(Cenario cenario, String observacao, boolean incluirObservacao) {
        Map<String, Object> pedido = new LinkedHashMap<>();
        pedido.put("clienteId", cenario.clienteId());
        pedido.put("restauranteId", cenario.restauranteId());
        pedido.put("enderecoEntregaId", cenario.enderecoId());
        pedido.put("itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 1)));
        if (incluirObservacao) {
            pedido.put("observacao", observacao);
        }
        return pedido;
    }
}
