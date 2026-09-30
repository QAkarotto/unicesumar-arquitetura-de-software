package br.edu.foodnow.controller;

import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Testes de caracterização da Atividade 06: fixam os valores EXATOS de distância, taxa, total,
 * tempo, região, zona e notificações produzidos pelo código antes da refatoração, inclusive
 * os resultados diferentes expostos em campos diferentes da API (distância do pedido, do
 * pagamento e da entrega). Os valores esperados foram obtidos executando a versão original.
 */
class CaracterizacaoEntregaIT extends ApiTestSupport {
    private static final double KM = 1e-5;
    private static final double REAIS = 0.001;

    @Test
    void fluxoNaZonaCentralDeveManterValoresExatos() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        JsonPath pedido = criarPedidoComDuasUnidades(cenario);
        long pedidoId = pedido.getLong("id");
        assertThat(pedido.getString("status")).isEqualTo("CRIADO");
        assertThat(pedido.getDouble("subtotal")).isCloseTo(60.00, within(REAIS));
        assertThat(pedido.getDouble("taxaEntrega")).isCloseTo(6.55, within(REAIS));
        assertThat(pedido.getDouble("valorTotal")).isCloseTo(66.55, within(REAIS));
        assertThat(pedido.getDouble("distanciaEntregaKm")).isCloseTo(1.515000000000093, within(KM));

        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);
        assertEmail("Pedido confirmado", "O pedido " + pedidoId
                + " foi confirmado para a região CENTRAL. Estimativa interna: 21 minutos.");

        JsonPath pagamento = pagar(pedidoId, "TOKEN_APROVADO");
        assertThat(pagamento.getString("status")).isEqualTo("APROVADO");
        assertThat(pagamento.getString("regiaoEntrega")).isEqualTo("CENTRAL");
        assertThat(pagamento.getDouble("distanciaValidadaKm")).isCloseTo(1.32, within(KM));
        assertThat(pagamento.getBoolean("riscoGeografico")).isFalse();
        assertEmail("Pagamento aprovado", "O pagamento do pedido " + pedidoId
                + " foi aprovado. Rota estimada pelo e-mail: 17 minutos."
                + " Região de entrega: CENTRAL [-25.095000,-50.160000].");

        JsonPath entrega = given().get("/entregas/pedido/{id}", pedidoId).then().statusCode(200)
                .extract().jsonPath();
        assertThat(entrega.getString("status")).isEqualTo("AGUARDANDO_ENTREGADOR");
        assertThat(entrega.getDouble("distanciaKm")).isCloseTo(1.32, within(KM));
        assertThat(entrega.getInt("tempoEstimadoMinutos")).isEqualTo(25);
        assertThat(entrega.getString("zonaEntrega")).isEqualTo("CENTRAL");
        assertThat(entrega.getString("codigoEntregadorExterno")).isEqualTo("COURIER-" + pedidoId);
        assertThat(entrega.getString("statusDespachoExterno")).isEqualTo("DRIVER_ASSIGNED");
        assertThat(entrega.getDouble("custoOperacional")).isCloseTo(3.56, within(REAIS));

        given().contentType(ContentType.JSON).body(Map.of("status", "EM_ROTA"))
                .patch("/entregas/{id}/status", entrega.getLong("id")).then().statusCode(200);
        assertEmail("Entrega iniciada", "O pedido " + pedidoId + " saiu para entrega.");
    }

    @Test
    void fluxoForaDoCentroComZonaExpandidaDeveManterValoresExatos() {
        Cenario cenario = criarCenarioComEnderecoDeEntrega("Uvaranas", "84010-100", -25.150, -50.200);

        JsonPath pedido = criarPedidoComDuasUnidades(cenario);
        long pedidoId = pedido.getLong("id");
        assertThat(pedido.getDouble("subtotal")).isCloseTo(60.00, within(REAIS));
        assertThat(pedido.getDouble("taxaEntrega")).isCloseTo(19.60, within(REAIS));
        assertThat(pedido.getDouble("valorTotal")).isCloseTo(79.60, within(REAIS));
        assertThat(pedido.getDouble("distanciaEntregaKm")).isCloseTo(10.35, within(KM));

        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);
        assertEmail("Pedido confirmado", "O pedido " + pedidoId
                + " foi confirmado para a região URBANA. Estimativa interna: 45 minutos.");

        JsonPath pagamento = pagar(pedidoId, "TOKEN_APROVADO");
        assertThat(pagamento.getString("regiaoEntrega")).isEqualTo("URBANA");
        assertThat(pagamento.getDouble("distanciaValidadaKm")).isCloseTo(8.62, within(KM));
        assertThat(pagamento.getBoolean("riscoGeografico")).isFalse();
        assertEmail("Pagamento aprovado", "O pagamento do pedido " + pedidoId
                + " foi aprovado. Rota estimada pelo e-mail: 43 minutos."
                + " Região de entrega: URBANA [-25.150000,-50.200000].");

        JsonPath entrega = given().get("/entregas/pedido/{id}", pedidoId).then().statusCode(200)
                .extract().jsonPath();
        assertThat(entrega.getDouble("distanciaKm")).isCloseTo(8.62, within(KM));
        assertThat(entrega.getInt("tempoEstimadoMinutos")).isEqualTo(54);
        assertThat(entrega.getString("zonaEntrega")).isEqualTo("EXPANDIDA");
        assertThat(entrega.getString("codigoEntregadorExterno")).isEqualTo("COURIER-" + pedidoId);
        assertThat(entrega.getDouble("custoOperacional")).isCloseTo(12.22, within(REAIS));
    }

    @Test
    void pagamentoRejeitadoDeveManterMensagemGeograficaESemEntrega() {
        Cenario cenario = criarCenarioComEnderecoDeEntrega("Uvaranas", "84010-100", -25.150, -50.200);
        long pedidoId = criarPedidoComDuasUnidades(cenario).getLong("id");
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);

        JsonPath pagamento = pagar(pedidoId, "REJEITADO");

        assertThat(pagamento.getString("status")).isEqualTo("REJEITADO");
        assertThat(pagamento.getString("regiaoEntrega")).isEqualTo("URBANA");
        assertThat(pagamento.getDouble("distanciaValidadaKm")).isCloseTo(8.62, within(KM));
        assertEmail("Pagamento rejeitado", "O pagamento do pedido " + pedidoId
                + " foi rejeitado na zona EXPANDIDA. Região de entrega: URBANA [-25.150000,-50.200000].");
        given().get("/entregas/pedido/{id}", pedidoId).then().statusCode(404);
    }

    @Test
    void simulacoesDeAtendimentoEDeEntregaDevemManterValoresExatos() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        JsonPath atendimento = given().get("/clientes/{c}/atendimento/{r}",
                cenario.clienteId(), cenario.restauranteId()).then().statusCode(200).extract().jsonPath();
        assertThat(atendimento.getBoolean("atendido")).isTrue();
        assertThat(atendimento.getDouble("distanciaClienteKm")).isCloseTo(1.150256761130213, within(KM));
        assertThat(atendimento.getDouble("distanciaEnderecoKm")).isCloseTo(1.1119721950210546, within(KM));
        assertThat(atendimento.getDouble("distanciaProvedorKm")).isCloseTo(1.32, within(KM));
        assertThat(atendimento.getString("zonaProvedor")).isEqualTo("CENTRAL");
        assertThat(atendimento.getDouble("taxaCliente")).isCloseTo(5.17, within(REAIS));
        assertThat(atendimento.getInt("tempoEstimadoMinutos")).isEqualTo(17);

        JsonPath simulacao = given().contentType(ContentType.JSON)
                .body(Map.of("destino", endereco("Rua para Simulação", -25.096, -50.161)))
                .post("/restaurantes/{id}/simular-entrega", cenario.restauranteId())
                .then().statusCode(200).extract().jsonPath();
        assertThat(simulacao.getString("regiaoRestaurante")).isEqualTo("PROXIMA");
        assertThat(simulacao.getString("zonaProvedor")).isEqualTo("CENTRAL");
        assertThat(simulacao.getInt("tempoEstimadoMinutos")).isEqualTo(17);
        assertThat(simulacao.getDouble("distanciaRestauranteKm")).isCloseTo(1.1936239669905606, within(KM));
        assertThat(simulacao.getDouble("distanciaEnderecosKm")).isCloseTo(1.1497507716025681, within(KM));
        assertThat(simulacao.getDouble("distanciaProvedorKm")).isCloseTo(1.37, within(KM));
        assertThat(simulacao.getDouble("taxaRestaurante")).isCloseTo(5.61, within(REAIS));
        assertThat(simulacao.getDouble("taxaEndereco")).isCloseTo(5.42, within(REAIS));
    }

    // ---------- apoio ----------

    private Cenario criarCenarioComEnderecoDeEntrega(String bairro, String cep, double latitude, double longitude) {
        long clienteId = given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Cliente API", "email", "cliente@foodnow.test"))
                .post("/clientes").then().statusCode(201).extract().jsonPath().getLong("id");
        Map<String, Object> enderecoCliente = Map.of("logradouro", "Rua do Cliente", "numero", "10",
                "bairro", bairro, "cidade", "Ponta Grossa", "cep", cep,
                "latitude", latitude, "longitude", longitude);
        long enderecoId = given().contentType(ContentType.JSON).body(enderecoCliente)
                .post("/clientes/{id}/enderecos", clienteId).then().statusCode(201)
                .extract().jsonPath().getLong("enderecos[0].id");
        long restauranteId = given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Cantina FoodNow", "raioEntregaKm", 25.0,
                        "endereco", endereco("Rua da Cantina", -25.100, -50.150)))
                .post("/restaurantes").then().statusCode(201).extract().jsonPath().getLong("id");
        long produtoId = given().contentType(ContentType.JSON)
                .body(Map.of("nome", "Pizza", "preco", new java.math.BigDecimal("30.00"), "disponivel", true))
                .post("/restaurantes/{id}/produtos", restauranteId).then().statusCode(201)
                .extract().jsonPath().getLong("id");
        return new Cenario(clienteId, enderecoId, restauranteId, produtoId);
    }

    private JsonPath criarPedidoComDuasUnidades(Cenario cenario) {
        Map<String, Object> pedido = Map.of(
                "clienteId", cenario.clienteId(), "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
        return given().contentType(ContentType.JSON).body(pedido).post("/pedidos")
                .then().statusCode(201).extract().jsonPath();
    }

    private JsonPath pagar(long pedidoId, String token) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", token))
                .post("/pagamentos").then().statusCode(201).extract().jsonPath();
    }

    private void assertEmail(String assunto, String mensagem) {
        assertThat(emailClient.getEnviadas())
                .filteredOn(notificacao -> assunto.equals(notificacao.getAssunto()))
                .singleElement()
                .satisfies((Notificacao notificacao) -> {
                    assertThat(notificacao.getDestinatario()).isEqualTo("cliente@foodnow.test");
                    assertThat(notificacao.getMensagem()).isEqualTo(mensagem);
                });
    }
}
