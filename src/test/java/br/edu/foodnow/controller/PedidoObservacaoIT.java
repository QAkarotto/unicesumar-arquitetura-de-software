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

/**
 * Testes de API da evolução do contrato de pedidos: observação opcional (até 500 caracteres)
 * informada somente na criação e devolvida em todas as respostas que representam o pedido.
 */
class PedidoObservacaoIT extends ApiTestSupport {

    private static final String OBSERVACAO = "Não incluir talheres";

    @Test
    void deveCriarPedidoComObservacaoEDevolveLaNaResposta() {
        Cenario cenario = cenarioAtendido();

        given().contentType(ContentType.JSON).body(pedidoCom(cenario, OBSERVACAO))
                .post("/pedidos").then().statusCode(201)
                .body("status", equalTo("CRIADO"))
                .body("observacao", equalTo(OBSERVACAO));
    }

    @Test
    void deveRecuperarObservacaoNaConsultaDoPedido() {
        Cenario cenario = cenarioAtendido();
        long pedidoId = criarPedidoCom(cenario, OBSERVACAO);

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("id", equalTo((int) pedidoId))
                .body("observacao", equalTo(OBSERVACAO));
    }

    @Test
    void deveManterObservacaoAoAdicionarItemEConfirmarPedido() {
        Cenario cenario = cenarioAtendido();
        long pedidoId = criarPedidoCom(cenario, OBSERVACAO);

        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("itens.size()", equalTo(2))
                .body("observacao", equalTo(OBSERVACAO));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"))
                .body("observacao", equalTo(OBSERVACAO));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"))
                .body("observacao", equalTo(OBSERVACAO));
    }

    @Test
    void deveIgnorarObservacaoEnviadaAoAdicionarItem() {
        Cenario cenario = cenarioAtendido();
        long pedidoId = criarPedidoCom(cenario, OBSERVACAO);

        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1, "observacao", "Trocar observação"))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(OBSERVACAO));
    }

    @Test
    void deveCriarPedidoSemObservacaoMantendoContratoAnterior() {
        Cenario cenario = cenarioAtendido();
        long pedidoId = criarPedido(cenario);

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("CRIADO"))
                .body("$", not(hasKey("observacao")));
        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
    }

    @Test
    void deveTratarObservacaoNulaComoAusente() {
        Cenario cenario = cenarioAtendido();

        given().contentType(ContentType.JSON).body(pedidoCom(cenario, null))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")));
    }

    @Test
    void deveTratarObservacaoVaziaOuEmBrancoComoAusente() {
        Cenario cenario = cenarioAtendido();

        long vazioId = criarPedidoCom(cenario, "");
        long brancoId = criarPedidoCom(cenario, "   ");

        given().get("/pedidos/{id}", vazioId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
        given().get("/pedidos/{id}", brancoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
    }

    @Test
    void deveAceitarObservacaoComExatamente500Caracteres() {
        Cenario cenario = cenarioAtendido();
        String observacao = "a".repeat(500);

        long pedidoId = given().contentType(ContentType.JSON).body(pedidoCom(cenario, observacao))
                .post("/pedidos").then().statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract().jsonPath().getLong("id");
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveRejeitarObservacaoCom501Caracteres() {
        Cenario cenario = cenarioAtendido();

        given().contentType(ContentType.JSON).body(pedidoCom(cenario, "a".repeat(501)))
                .post("/pedidos").then().statusCode(400)
                .body("status", equalTo(400))
                .body("erro", equalTo("VALIDATION_ERROR"))
                .body("mensagem", equalTo("observacao: deve ter no máximo 500 caracteres"));
    }

    private Cenario cenarioAtendido() {
        return criarCenario(true, 25, -25.095, -50.160);
    }

    private long criarPedidoCom(Cenario cenario, String observacao) {
        return given().contentType(ContentType.JSON).body(pedidoCom(cenario, observacao))
                .post("/pedidos").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    private Map<String, Object> pedidoCom(Cenario cenario, String observacao) {
        Map<String, Object> pedido = new HashMap<>();
        pedido.put("clienteId", cenario.clienteId());
        pedido.put("restauranteId", cenario.restauranteId());
        pedido.put("enderecoEntregaId", cenario.enderecoId());
        pedido.put("itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
        pedido.put("observacao", observacao);
        return pedido;
    }
}
