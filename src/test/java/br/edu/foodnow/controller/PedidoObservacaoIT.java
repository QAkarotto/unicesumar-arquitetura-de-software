package br.edu.foodnow.controller;

import br.edu.foodnow.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class PedidoObservacaoIT extends ApiTestSupport {
    @Autowired
    private PedidoRepository pedidoRepository;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Não incluir talheres", "   ", "  Sem cebola  "})
    void devePersistirEManterObservacaoNasOperacoesDoPedido(String observacao) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> request = pedido(cenario);
        request.put("observacao", observacao);
        verificarFluxo(cenario, request, observacao);
    }

    @Test
    void devePreservarContratoQuandoCampoNaoForEnviado() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        verificarFluxo(cenario, pedido(cenario), null);
    }

    @Test
    void deveAceitarERecuperarObservacaoCom500Caracteres() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> request = pedido(cenario);
        String observacao = "á".repeat(500);
        request.put("observacao", observacao);
        verificarFluxo(cenario, request, observacao);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", " "})
    void deveRejeitar501CaracteresSemCriarPedido(String caractere) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> request = pedido(cenario);
        request.put("observacao", caractere.repeat(501));
        long quantidadeAnterior = pedidoRepository.count();
        given().contentType(ContentType.JSON).body(request).post("/pedidos")
                .then().statusCode(400)
                .body("status", equalTo(400))
                .body("erro", equalTo("VALIDATION_ERROR"))
                .body("mensagem", containsString("observacao"));
        assertThat(pedidoRepository.count()).isEqualTo(quantidadeAnterior);
    }

    @Test
    void naoDeveSubstituirObservacaoAoAdicionarItem() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> request = pedido(cenario);
        request.put("observacao", "Sem talheres");
        long id = given().contentType(ContentType.JSON).body(request).post("/pedidos")
                .then().statusCode(201).extract().jsonPath().getLong("id");
        Response resposta = given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1,
                        "observacao", "Substituir texto"))
                .post("/pedidos/{id}/itens", id);
        // O DTO de item continua ignorando campos desconhecidos, como na base.
        conferir(resposta, 200, "Sem talheres");
        conferir(given().get("/pedidos/{id}", id), 200, "Sem talheres");
    }

    private void verificarFluxo(Cenario cenario, Map<String, Object> request, String observacao) {
        Response criado = given().contentType(ContentType.JSON).body(request).post("/pedidos");
        conferir(criado, 201, observacao);
        criado.then().body("status", equalTo("CRIADO")).body("subtotal", equalTo(60.0F));
        long id = criado.jsonPath().getLong("id");
        conferir(given().get("/pedidos/{id}", id), 200, observacao);
        Response comItem = given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", id);
        conferir(comItem, 200, observacao);
        comItem.then().body("subtotal", equalTo(90.0F));
        Response confirmado = given().post("/pedidos/{id}/confirmar", id);
        conferir(confirmado, 200, observacao);
        confirmado.then().body("status", equalTo("CONFIRMADO"));
        conferir(given().get("/pedidos/{id}", id), 200, observacao);
    }

    private void conferir(Response resposta, int status, String observacao) {
        resposta.then().statusCode(status);
        Map<String, Object> json = resposta.jsonPath().getMap("$");
        if (observacao == null) {
            assertThat(json).doesNotContainKey("observacao");
        } else {
            assertThat(json).containsEntry("observacao", observacao);
        }
    }

    private Map<String, Object> pedido(Cenario cenario) {
        return new LinkedHashMap<>(Map.of("clienteId", cenario.clienteId(),
                "restauranteId", cenario.restauranteId(), "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2))));
    }
}
