package br.edu.foodnow.controller;

import br.edu.foodnow.FoodNowApplication;
import br.edu.foodnow.integration.FakeEmailClient;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

@SpringBootTest(classes = FoodNowApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class ApiTestSupport {
    @LocalServerPort
    private int port;

    @Autowired
    protected FakeEmailClient emailClient;

    @BeforeEach
    void configurarRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        emailClient.limpar();
    }

    protected Cenario criarCenario(boolean produtoDisponivel, double raioEntregaKm,
                                   double latitudeCliente, double longitudeCliente) {
        long clienteId = given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Cliente API", "email", "cliente@foodnow.test"))
                .post("/clientes").then().statusCode(201).extract().jsonPath().getLong("id");

        long enderecoId = given().contentType(ContentType.JSON)
                .body(endereco("Rua do Cliente", latitudeCliente, longitudeCliente))
                .post("/clientes/{id}/enderecos", clienteId).then().statusCode(201)
                .extract().jsonPath().getLong("enderecos[0].id");

        Map<String, Object> restaurante = Map.of(
                "nome", "Cantina FoodNow",
                "raioEntregaKm", raioEntregaKm,
                "endereco", endereco("Rua da Cantina", -25.100, -50.150));
        long restauranteId = given().contentType(ContentType.JSON).body(restaurante)
                .post("/restaurantes").then().statusCode(201).extract().jsonPath().getLong("id");

        Map<String, Object> produto = Map.of("nome", "Pizza", "preco", new BigDecimal("30.00"),
                "disponivel", produtoDisponivel);
        long produtoId = given().contentType(ContentType.JSON).body(produto)
                .post("/restaurantes/{id}/produtos", restauranteId).then().statusCode(201)
                .extract().jsonPath().getLong("id");
        return new Cenario(clienteId, enderecoId, restauranteId, produtoId);
    }

    protected long criarPedido(Cenario cenario) {
        Map<String, Object> pedido = Map.of(
                "clienteId", cenario.clienteId(),
                "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
        return given().contentType(ContentType.JSON).body(pedido)
                .post("/pedidos").then().statusCode(201).extract().jsonPath().getLong("id");
    }

    protected Map<String, Object> endereco(String logradouro, double latitude, double longitude) {
        return Map.of("logradouro", logradouro, "numero", "10", "bairro", "Centro",
                "cidade", "Ponta Grossa", "cep", "84000-000",
                "latitude", latitude, "longitude", longitude);
    }

    protected record Cenario(long clienteId, long enderecoId, long restauranteId, long produtoId) {
    }
}
