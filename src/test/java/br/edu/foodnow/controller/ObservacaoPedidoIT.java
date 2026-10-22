package br.edu.foodnow.controller;

import br.edu.foodnow.repository.PedidoRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

class ObservacaoPedidoIT extends ApiTestSupport {
    private static final String OBSERVACAO = "Não incluir talheres";

    @Autowired
    private PedidoRepository pedidoRepository;

    @Test
    void deveManterObservacaoEmTodasAsOperacoesDoPedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = given().contentType(ContentType.JSON).body(pedido(cenario, OBSERVACAO))
                .post("/pedidos").then().statusCode(201)
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
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_OK"))
                .post("/pagamentos").then().statusCode(201);
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGO"))
                .body("observacao", equalTo(OBSERVACAO));

        assertThat(pedidoRepository.findById(pedidoId)).get()
                .extracting("observacao").isEqualTo(OBSERVACAO);
    }

    @Test
    void deveCriarPedidoSemObservacaoSemExporOCampo() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = given().contentType(ContentType.JSON).body(pedido(cenario, null))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")))
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
    void deveTratarObservacaoNullComoAusente() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> corpo = new HashMap<>(pedido(cenario, null));
        corpo.put("observacao", null);

        long pedidoId = given().contentType(ContentType.JSON).body(corpo)
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")))
                .extract().jsonPath().getLong("id");

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
        assertThat(pedidoRepository.findById(pedidoId)).get()
                .extracting("observacao").isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\t"})
    void deveTratarObservacaoEmBrancoComoAusente(String observacao) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long pedidoId = given().contentType(ContentType.JSON).body(pedido(cenario, observacao))
                .post("/pedidos").then().statusCode(201)
                .body("$", not(hasKey("observacao")))
                .extract().jsonPath().getLong("id");

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("$", not(hasKey("observacao")));
        assertThat(pedidoRepository.findById(pedidoId)).get()
                .extracting("observacao").isNull();
    }

    @Test
    void deveAceitarObservacaoCom500Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String observacao = "a".repeat(500);

        long pedidoId = given().contentType(ContentType.JSON).body(pedido(cenario, observacao))
                .post("/pedidos").then().statusCode(201)
                .body("observacao", equalTo(observacao))
                .extract().jsonPath().getLong("id");

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(observacao));
    }

    @Test
    void deveRejeitarObservacaoCom501CaracteresSemCriarPedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidosAntes = pedidoRepository.count();

        given().contentType(ContentType.JSON).body(pedido(cenario, "a".repeat(501)))
                .post("/pedidos").then().statusCode(400)
                .body("status", equalTo(400))
                .body("erro", equalTo("VALIDATION_ERROR"))
                .body("mensagem", startsWith("observacao: "));

        assertThat(pedidoRepository.count()).isEqualTo(pedidosAntes);
    }

    @Test
    void naoDeveAlterarObservacaoAoAdicionarItem() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = given().contentType(ContentType.JSON).body(pedido(cenario, OBSERVACAO))
                .post("/pedidos").then().statusCode(201).extract().jsonPath().getLong("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1, "observacao", "Trocar"))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(OBSERVACAO));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("observacao", equalTo(OBSERVACAO));
    }

    private Map<String, Object> pedido(Cenario cenario, String observacao) {
        Map<String, Object> corpo = new HashMap<>(Map.of(
                "clienteId", cenario.clienteId(), "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2))));
        if (observacao != null) {
            corpo.put("observacao", observacao);
        }
        return corpo;
    }
}
