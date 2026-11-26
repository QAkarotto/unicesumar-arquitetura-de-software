package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

class FoodNowErrorsIT extends ApiTestSupport {

    @Test
    void deveRejeitarProdutoIndisponivel() {
        Cenario cenario = criarCenario(false, 25, -25.095, -50.160);
        Map<String, Object> pedido = requisicaoPedido(cenario, List.of(
                Map.of("produtoId", cenario.produtoId(), "quantidade", 1)));

        given().contentType(ContentType.JSON).body(pedido).post("/pedidos")
                .then().statusCode(422)
                .body("mensagem", containsString("Produto indisponível"));
    }

    @Test
    void deveRejeitarClienteForaDaAreaDeEntrega() {
        Cenario cenario = criarCenario(true, 2, -25.500, -50.500);
        given().contentType(ContentType.JSON)
                .body(requisicaoPedido(cenario, List.of(
                        Map.of("produtoId", cenario.produtoId(), "quantidade", 1))))
                .post("/pedidos").then().statusCode(422)
                .body("mensagem", equalTo("Endereço fora da área de entrega"));
    }

    @Test
    void deveRegistrarPagamentoRejeitadoSemCriarEntrega() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);

        given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "CARTAO_CREDITO", "token", "REJEITADO"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("REJEITADO"));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGAMENTO_REJEITADO"));
        given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(404);

        assertThat(emailClient.getEnviadas()).extracting("assunto")
                .contains("Pedido confirmado", "Pagamento rejeitado");
    }

    @Test
    void deveValidarPedidoSemItensERecursosInexistentes() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        given().contentType(ContentType.JSON).body(requisicaoPedido(cenario, List.of()))
                .post("/pedidos").then().statusCode(400)
                .body("erro", equalTo("VALIDATION_ERROR"));
        given().get("/clientes/{id}", 999999).then().statusCode(404)
                .body("mensagem", equalTo("Cliente não encontrado"));
    }

    private Map<String, Object> requisicaoPedido(Cenario cenario, List<Map<String, Object>> itens) {
        return Map.of("clienteId", cenario.clienteId(), "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", cenario.enderecoId(), "itens", itens);
    }
}
