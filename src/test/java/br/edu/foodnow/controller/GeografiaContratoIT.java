package br.edu.foodnow.controller;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class GeografiaContratoIT extends ApiTestSupport {
    @ParameterizedTest
    @CsvSource({
            "central, Centro, -25.095, -50.160, TOKEN_OK",
            "urbana, Uvaranas, -25.110, -50.180, TOKEN_OK",
            "expandida, Uvaranas, -25.180, -50.240, REJEITADO",
            "sem-entregador, Uvaranas, -25.500, -50.500, TOKEN_OK"
    })
    void devePreservarValoresRegioesPrazosENotificacoes(String nome, String bairro,
            double latitude, double longitude, String token) throws Exception {
        Cenario cenario = criarCenario(true, 200, -25.095, -50.160);
        Map<String, Object> destino = new LinkedHashMap<>(endereco("Destino", latitude, longitude));
        destino.put("bairro", bairro);
        long enderecoId = given().contentType(ContentType.JSON).body(destino)
                .post("/clientes/{id}/enderecos", cenario.clienteId()).then().statusCode(201)
                .extract().jsonPath().getLong("enderecos[1].id");
        Map<String, String> capturas = new TreeMap<>();
        capturar(capturas, "atendimento", given().get("/clientes/{clienteId}/atendimento/{restauranteId}",
                cenario.clienteId(), cenario.restauranteId()), 200);
        capturar(capturas, "simulacao", given().contentType(ContentType.JSON).body(Map.of("destino", destino))
                .post("/restaurantes/{id}/simular-entrega", cenario.restauranteId()), 200);
        Response criado = given().contentType(ContentType.JSON).body(pedido(cenario, enderecoId))
                .post("/pedidos");
        capturar(capturas, "pedido-criado", criado, 201);
        long pedidoId = criado.jsonPath().getLong("id");
        capturar(capturas, "pedido-com-item", given().contentType(ContentType.JSON)
                .body(Map.of("produtoId", cenario.produtoId(), "quantidade", 1))
                .post("/pedidos/{id}/itens", pedidoId), 200);
        capturar(capturas, "pedido-confirmado", given().post("/pedidos/{id}/confirmar", pedidoId), 200);
        capturar(capturas, "pagamento", given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", token))
                .post("/pagamentos"), 201);
        Response entrega = given().get("/entregas/pedido/{pedidoId}", pedidoId);
        if ("REJEITADO".equals(token)) {
            capturar(capturas, "entrega", entrega, 404);
        } else {
            capturar(capturas, "entrega", entrega, 200);
            long entregaId = entrega.jsonPath().getLong("id");
            capturar(capturas, "entrega-em-rota", given().contentType(ContentType.JSON)
                    .body(Map.of("status", "EM_ROTA")).patch("/entregas/{id}/status", entregaId), 200);
            capturar(capturas, "entrega-concluida", given().contentType(ContentType.JSON)
                    .body(Map.of("status", "ENTREGUE")).patch("/entregas/{id}/status", entregaId), 200);
        }
        capturar(capturas, "pedido-final", given().get("/pedidos/{id}", pedidoId), 200);
        int indice = 0;
        for (var email : emailClient.getEnviadas()) {
            capturas.put("email-" + indice++, email.getDestinatario() + " | " + email.getAssunto() + " | "
                    + email.getMensagem().replace("pedido " + pedidoId, "pedido <id>"));
        }
        conferirReferencia(nome, capturas);
    }

    @Test
    void deveConsiderarEnderecoPrincipalMesmoComDestinoProximo() {
        Cenario cenario = criarCenario(true, 5, -25.5, -50.5);
        long destino = given().contentType(ContentType.JSON).body(endereco("Perto", -25.095, -50.160))
                .post("/clientes/{id}/enderecos", cenario.clienteId()).then().statusCode(201)
                .extract().jsonPath().getLong("enderecos[1].id");
        given().contentType(ContentType.JSON).body(pedido(cenario, destino)).post("/pedidos")
                .then().statusCode(422).body("mensagem", equalTo("Endereço fora da área de entrega"));
    }

    @Test
    void deveRejeitarRegiaoDiferenteMesmoComCoordenadasProximas() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> destino = new LinkedHashMap<>(endereco("Outro CEP", -25.095, -50.160));
        destino.put("cep", "84100-000");
        long enderecoId = given().contentType(ContentType.JSON).body(destino)
                .post("/clientes/{id}/enderecos", cenario.clienteId()).then().statusCode(201)
                .extract().jsonPath().getLong("enderecos[1].id");
        given().contentType(ContentType.JSON).body(pedido(cenario, enderecoId)).post("/pedidos")
                .then().statusCode(422).body("mensagem", equalTo("Endereço fora da área de entrega"));
    }

    private Map<String, Object> pedido(Cenario cenario, long enderecoId) {
        return Map.of("clienteId", cenario.clienteId(), "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", enderecoId,
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
    }

    private void capturar(Map<String, String> capturas, String etapa, Response resposta, int status) {
        resposta.then().statusCode(status);
        String json = resposta.asString()
                .replaceAll("\"(id|[a-zA-Z]+Id)\"\\s*:\\s*\\d+", "\"$1\":0")
                .replaceAll("(COURIER-|FN-)\\d+", "$1<id>");
        capturas.put(etapa, json);
    }

    private void conferirReferencia(String nome, Map<String, String> capturas) throws Exception {
        Properties referencia = new Properties();
        try (var arquivo = getClass().getResourceAsStream("/geografia/" + nome + ".properties")) {
            assertThat(arquivo).as("Referência capturada antes da refatoração: %s", nome).isNotNull();
            var leitor = new InputStreamReader(arquivo, StandardCharsets.UTF_8);
            referencia.load(leitor);
        }
        assertThat(capturas.keySet()).containsExactlyInAnyOrderElementsOf(referencia.stringPropertyNames());
        capturas.forEach((etapa, atual) -> {
            String esperado = referencia.getProperty(etapa);
            if (atual.startsWith("{")) {
                var configuracao = new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL);
                assertThat(new JsonPath(atual).using(configuracao).getMap("$")).as("%s: %s", nome, etapa)
                        .isEqualTo(new JsonPath(esperado).using(configuracao).getMap("$"));
            } else {
                assertThat(atual).as("%s: %s", nome, etapa).isEqualTo(esperado);
            }
        });
    }
}
