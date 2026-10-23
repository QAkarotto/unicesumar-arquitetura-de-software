package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class PedidoObservacaoIT extends ApiTestSupport {
    @Autowired
    private JdbcTemplate jdbc;

    static Stream<String> observacoesAceitas() {
        return Stream.of("Não incluir talheres", null, "", "   ", "a".repeat(500),
                "\uD83D\uDE00".repeat(500), "\uD801\uDC00".repeat(500),
                "a\uD83D\uDE00".repeat(250));
    }

    @ParameterizedTest
    @MethodSource("observacoesAceitas")
    void devePersistirEManterObservacaoEmTodasAsOperacoes(String observacao) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> pedido = pedido(cenario);
        pedido.put("observacao", observacao);
        conferirCiclo(cenario, pedido, observacao);
    }

    @Test
    void deveContinuarAceitandoPedidoSemOCampo() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        conferirCiclo(cenario, pedido(cenario), null);
    }

    @Test
    void deveRejeitar501CaracteresSemPersistirPedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> pedido = pedido(cenario);
        pedido.put("observacao", "a".repeat(501));
        Long antes = jdbc.queryForObject("select count(*) from pedido", Long.class);

        given().contentType(ContentType.JSON).body(pedido).post("/pedidos")
                .then().statusCode(400).body("erro", equalTo("VALIDATION_ERROR"));

        assertThat(jdbc.queryForObject("select count(*) from pedido", Long.class)).isEqualTo(antes);
    }

    static Stream<String> observacoesUnicodeExcedidas() {
        return Stream.of("\uD83D\uDE00".repeat(501), "\uD801\uDC00".repeat(501),
                "a\uD83D\uDE00".repeat(250) + "a");
    }

    @ParameterizedTest
    @MethodSource("observacoesUnicodeExcedidas")
    void deveRejeitar501PontosDeCodigoUnicode(String observacao) {
        assertThat(observacao.codePointCount(0, observacao.length())).isEqualTo(501);
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> pedido = pedido(cenario);
        pedido.put("observacao", observacao);
        Long antes = jdbc.queryForObject("select count(*) from pedido", Long.class);

        given().contentType(ContentType.JSON).body(pedido).post("/pedidos")
                .then().statusCode(400).body("erro", equalTo("VALIDATION_ERROR"));

        assertThat(jdbc.queryForObject("select count(*) from pedido", Long.class)).isEqualTo(antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "12.5", "true", "false", "{}", "[]"})
    void deveRejeitarTiposJsonNaoTextuais(String valorJson) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        String corpo = """
                {"clienteId":%d,"restauranteId":%d,"enderecoEntregaId":%d,
                 "itens":[{"produtoId":%d,"quantidade":1}],"observacao":%s}
                """.formatted(cenario.clienteId(), cenario.restauranteId(), cenario.enderecoId(),
                cenario.produtoId(), valorJson);
        Long antes = jdbc.queryForObject("select count(*) from pedido", Long.class);

        // O Spring MVC trata a falha de desserialização como 400; não há schema de erro próprio.
        given().contentType(ContentType.JSON).body(corpo).post("/pedidos").then().statusCode(400);

        assertThat(jdbc.queryForObject("select count(*) from pedido", Long.class)).isEqualTo(antes);
    }

    @Test
    void operacoesPosterioresNaoDevemEditarObservacao() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> pedido = pedido(cenario);
        pedido.put("observacao", "Original");
        long id = given().contentType(ContentType.JSON).body(pedido).post("/pedidos")
                .then().statusCode(201).extract().jsonPath().getLong("id");

        Response item = given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1, "observacao", "Alterada"))
                .post("/pedidos/{id}/itens", id);
        conferirObservacao(item, 200, "Original");
        Response confirmado = given().contentType(ContentType.JSON)
                .body(Map.of("observacao", "Alterada"))
                .post("/pedidos/{id}/confirmar", id);
        conferirObservacao(confirmado, 200, "Original");
        conferirObservacao(given().get("/pedidos/{id}", id), 200, "Original");
        assertThat(observacaoPersistida(id)).isEqualTo("Original");
    }

    private Map<String, Object> pedido(Cenario cenario) {
        return new LinkedHashMap<>(Map.of("clienteId", cenario.clienteId(),
                "restauranteId", cenario.restauranteId(), "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2))));
    }

    private void conferirCiclo(Cenario cenario, Map<String, Object> pedido, String observacao) {
        Response criado = given().contentType(ContentType.JSON).body(pedido).post("/pedidos");
        conferirObservacao(criado, 201, observacao);
        long id = criado.jsonPath().getLong("id");
        assertThat(observacaoPersistida(id)).isEqualTo(observacao);
        conferirObservacao(given().get("/pedidos/{id}", id), 200, observacao);

        Response comItem = given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", id);
        conferirObservacao(comItem, 200, observacao);
        conferirObservacao(given().get("/pedidos/{id}", id), 200, observacao);
        Response confirmado = given().post("/pedidos/{id}/confirmar", id);
        conferirObservacao(confirmado, 200, observacao);
        confirmado.then().body("status", equalTo("CONFIRMADO"));
        conferirObservacao(given().get("/pedidos/{id}", id), 200, observacao);
        assertThat(observacaoPersistida(id)).isEqualTo(observacao);
    }

    private String observacaoPersistida(long id) {
        return jdbc.queryForObject("select observacao from pedido where id = ?", String.class, id);
    }

    private void conferirObservacao(Response resposta, int status, String observacao) {
        resposta.then().statusCode(status);
        Map<String, Object> json = resposta.jsonPath().getMap("$");
        if (observacao == null) {
            assertThat(json).doesNotContainKey("observacao");
        } else {
            assertThat(json).containsEntry("observacao", observacao);
        }
    }
}
