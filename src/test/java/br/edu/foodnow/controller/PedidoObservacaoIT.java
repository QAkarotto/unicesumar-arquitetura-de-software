
package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class PedidoObservacaoIT extends ApiTestSupport {

    @Test
    void deveCriarConsultarAdicionarItemEConfirmarMantendoObservacao() {
        Cenario cenario = criarCenario(
                true, 25, -25.095, -50.160
        );

        String observacao = "Não incluir talheres";

        Map<String, Object> pedido = requisicaoBase(cenario);
        pedido.put("observacao", observacao);

        long pedidoId = given()
                .contentType(ContentType.JSON)
                .body(pedido)
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract()
                .jsonPath()
                .getLong("id");

        // Consulta do pedido.
        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));

        // Inclusão de um novo item.
        given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "produtoId", cenario.produtoId(),
                        "quantidade", 1
                ))
                .post("/pedidos/{id}/itens", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));

        // Confirmação do pedido.
        given()
                .post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));

        // Consulta final para confirmar a persistência.
        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveAceitarCriacaoSemCampoObservacao() {
        Cenario cenario = criarCenario(
                true, 25, -25.095, -50.160
        );

        long pedidoId = given()
                .contentType(ContentType.JSON)
                .body(requisicaoBase(cenario))
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", nullValue())
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", nullValue());
    }

    @Test
    void deveAceitarObservacaoNula() {
        Cenario cenario = criarCenario(
                true, 25, -25.095, -50.160
        );

        Map<String, Object> pedido = requisicaoBase(cenario);
        pedido.put("observacao", null);

        given()
                .contentType(ContentType.JSON)
                .body(pedido)
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", nullValue());
    }

    @Test
    void deveNormalizarTextoVazioESomenteEspacos() {
        for (String observacao : List.of("", "   ")) {
            Cenario cenario = criarCenario(
                    true, 25, -25.095, -50.160
            );

            Map<String, Object> pedido = requisicaoBase(cenario);
            pedido.put("observacao", observacao);

            given()
                    .contentType(ContentType.JSON)
                    .body(pedido)
                    .post("/pedidos")
                    .then()
                    .statusCode(201)
                    .body("observacao", nullValue());
        }
    }

    @Test
    void deveAceitarObservacaoComExatamente500Caracteres() {
        Cenario cenario = criarCenario(
                true, 25, -25.095, -50.160
        );

        String observacao = "A".repeat(500);

        Map<String, Object> pedido = requisicaoBase(cenario);
        pedido.put("observacao", observacao);

        long pedidoId = given()
                .contentType(ContentType.JSON)
                .body(pedido)
                .post("/pedidos")
                .then()
                .statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveRejeitarObservacaoCom501Caracteres() {
        Cenario cenario = criarCenario(
                true, 25, -25.095, -50.160
        );

        String observacao = "A".repeat(501);

        Map<String, Object> pedido = requisicaoBase(cenario);
        pedido.put("observacao", observacao);

        given()
                .contentType(ContentType.JSON)
                .body(pedido)
                .post("/pedidos")
                .then()
                .statusCode(400)
                .body("erro", equalTo("VALIDATION_ERROR"));
    }

    private Map<String, Object> requisicaoBase(Cenario cenario) {
        Map<String, Object> pedido = new LinkedHashMap<>();

        pedido.put("clienteId", cenario.clienteId());
        pedido.put("restauranteId", cenario.restauranteId());
        pedido.put("enderecoEntregaId", cenario.enderecoId());

        pedido.put("itens", List.of(
                Map.of(
                        "produtoId", cenario.produtoId(),
                        "quantidade", 2
                )
        ));

        return pedido;
    }
}
