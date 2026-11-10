package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

/**
 * Testes de API da observação do pedido (contrato 1.1.0).
 */
class ObservacaoPedidoIT extends ApiTestSupport {
    private static final String OBSERVACAO = "Não incluir talheres";

    @Test
    void deveArmazenarERetornarObservacaoEmTodasAsOperacoesDePedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = criar(cenario, OBSERVACAO).statusCode(201)
                .body("observacao", equalTo(OBSERVACAO))
                .body("status", equalTo("CRIADO"))
                .body("subtotal", equalTo(60.0F))
                .extract().jsonPath().getLong("id");

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(OBSERVACAO));
        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("subtotal", equalTo(90.0F))
                .body("observacao", equalTo(OBSERVACAO));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"))
                .body("observacao", equalTo(OBSERVACAO));
        given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_APROVADO"))
                .post("/pagamentos").then().statusCode(201);
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGO"))
                .body("observacao", equalTo(OBSERVACAO));
    }

    @Test
    void naoDevePermitirEditarObservacaoAoAdicionarItem() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criar(cenario, OBSERVACAO).statusCode(201).extract().jsonPath().getLong("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1, "observacao", "Outra"))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(OBSERVACAO));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(OBSERVACAO));
    }

    @Test
    void pedidoSemObservacaoDeveContinuarFuncionandoSemOCampoNaResposta() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = given().contentType(ContentType.JSON).body(pedido(cenario))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")))
                .body("status", equalTo("CRIADO"))
                .body("subtotal", equalTo(60.0F))
                .extract().jsonPath().getLong("id");

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
    }

    @Test
    void observacaoNulaDeveSerTratadaComoAusente() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        String corpo = """
                {"clienteId": %d, "restauranteId": %d, "enderecoEntregaId": %d,
                 "itens": [{"produtoId": %d, "quantidade": 2}], "observacao": null}
                """.formatted(cenario.clienteId(), cenario.restauranteId(), cenario.enderecoId(),
                cenario.produtoId());

        long pedidoId = given().contentType(ContentType.JSON).body(corpo).post("/pedidos").then()
                .statusCode(201)
                .body("$", not(hasKey("observacao")))
                .extract().jsonPath().getLong("id");
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   \t  "})
    void observacaoVaziaOuEmBrancoDeveSerTratadaComoAusente(String observacao) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = criar(cenario, observacao).statusCode(201)
                .body("$", not(hasKey("observacao")))
                .extract().jsonPath().getLong("id");
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
    }

    @Test
    void deveRemoverEspacosDasBordasDaObservacao() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = criar(cenario, "  Sem cebola, por favor  ").statusCode(201)
                .body("observacao", equalTo("Sem cebola, por favor"))
                .extract().jsonPath().getLong("id");
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo("Sem cebola, por favor"));
    }

    @Test
    void deveAceitarObservacaoCom500Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "a".repeat(500);

        long pedidoId = criar(cenario, observacao).statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract().jsonPath().getLong("id");
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao.length()", equalTo(500));
    }

    @Test
    void deveRejeitarObservacaoCom501Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        criar(cenario, "a".repeat(501)).statusCode(400)
                .body("status", equalTo(400))
                .body("erro", equalTo("VALIDATION_ERROR"))
                .body("mensagem", startsWith("observacao:"));
    }

    @Test
    void limiteDeveSerAplicadoAoValorRecebidoAntesDeRemoverEspacos() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        criar(cenario, " " + "a".repeat(500)).statusCode(400)
                .body("erro", equalTo("VALIDATION_ERROR"))
                .body("mensagem", startsWith("observacao:"));
    }

    private ValidatableResponse criar(Cenario cenario, String observacao) {
        Map<String, Object> corpo = new HashMap<>(pedido(cenario));
        corpo.put("observacao", observacao);
        return given().contentType(ContentType.JSON).body(corpo).post("/pedidos").then();
    }

    private Map<String, Object> pedido(Cenario cenario) {
        return Map.of("clienteId", cenario.clienteId(), "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
    }
}
