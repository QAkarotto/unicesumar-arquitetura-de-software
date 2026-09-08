package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

class FoodNowFlowIT extends ApiTestSupport {

    @Test
    void deveExecutarFluxoCompletoDoPedidoAteAEntrega() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        long segundoEndereco = given().contentType(ContentType.JSON)
                .body(endereco("Avenida Principal", -25.096, -50.161))
                .post("/clientes/{id}/enderecos", cenario.clienteId()).then().statusCode(201)
                .extract().jsonPath().getLong("enderecos[1].id");
        given().put("/clientes/{clienteId}/enderecos/{enderecoId}/principal",
                        cenario.clienteId(), segundoEndereco)
                .then().statusCode(200).body("enderecos", hasSize(2));
        given().get("/clientes/{id}", cenario.clienteId()).then().statusCode(200)
                .body("nome", equalTo("Cliente API"));
        given().get("/restaurantes/{id}", cenario.restauranteId()).then().statusCode(200)
                .body("nome", equalTo("Cantina FoodNow"));
        given().get("/clientes/{clienteId}/atendimento/{restauranteId}",
                        cenario.clienteId(), cenario.restauranteId()).then().statusCode(200)
                .body("atendido", equalTo(true))
                .body("distanciaClienteKm", greaterThan(0.0F))
                .body("distanciaEnderecoKm", greaterThan(0.0F))
                .body("distanciaProvedorKm", greaterThan(0.0F));
        given().contentType(ContentType.JSON)
                .body(Map.of("destino", endereco("Rua para Simulação", -25.096, -50.161)))
                .post("/restaurantes/{id}/simular-entrega", cenario.restauranteId())
                .then().statusCode(200)
                .body("distanciaRestauranteKm", greaterThan(0.0F))
                .body("distanciaEnderecosKm", greaterThan(0.0F))
                .body("distanciaProvedorKm", greaterThan(0.0F));
        given().get("/produtos/{id}", cenario.produtoId()).then().statusCode(200)
                .body("disponivel", equalTo(true));
        given().contentType(ContentType.JSON).body(Map.of("disponivel", false))
                .patch("/produtos/{id}/disponibilidade", cenario.produtoId()).then().statusCode(200)
                .body("disponivel", equalTo(false));
        given().contentType(ContentType.JSON).body(Map.of("disponivel", true))
                .patch("/produtos/{id}/disponibilidade", cenario.produtoId()).then().statusCode(200);

        Map<String, Object> novoPedido = Map.of(
                "clienteId", cenario.clienteId(), "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", segundoEndereco,
                "itens", java.util.List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
        long pedidoId = given().contentType(ContentType.JSON).body(novoPedido)
                .post("/pedidos").then().statusCode(201)
                .body("status", equalTo("CRIADO"))
                .body("subtotal", equalTo(60.0F))
                .body("valorTotal", greaterThan(60.0F))
                .extract().jsonPath().getLong("id");

        given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId).then().statusCode(200)
                .body("subtotal", equalTo(90.0F));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        long pagamentoId = given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_APROVADO"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"))
                .body("regiaoEntrega", equalTo("CENTRAL"))
                .body("riscoGeografico", equalTo(false))
                .extract().jsonPath().getLong("id");
        given().get("/pagamentos/{id}", pagamentoId).then().statusCode(200)
                .body("pedidoId", equalTo((int) pedidoId));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGO"));

        long entregaId = given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(200)
                .body("status", equalTo("AGUARDANDO_ENTREGADOR"))
                .body("distanciaKm", greaterThan(0.0F))
                .body("zonaEntrega", equalTo("CENTRAL"))
                .body("statusDespachoExterno", equalTo("DRIVER_ASSIGNED"))
                .extract().jsonPath().getLong("id");
        given().get("/entregas/{id}", entregaId).then().statusCode(200)
                .body("pedidoId", equalTo((int) pedidoId));
        given().contentType(ContentType.JSON).body(Map.of("status", "EM_ROTA"))
                .patch("/entregas/{id}/status", entregaId).then().statusCode(200)
                .body("status", equalTo("EM_ROTA"));
        given().contentType(ContentType.JSON).body(Map.of("status", "ENTREGUE"))
                .patch("/entregas/{id}/status", entregaId).then().statusCode(200)
                .body("status", equalTo("ENTREGUE"));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("ENTREGUE"));

        assertThat(emailClient.getEnviadas()).extracting("assunto")
                .contains("Pedido confirmado", "Pagamento aprovado", "Entrega iniciada");
    }
}
