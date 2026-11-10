package br.edu.foodnow.controller;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confere requisições e respostas reais das operações de pedidos contra a especificação
 * {@code docs/openapi/foodnow-pedidos.yaml}, garantindo que o contrato documentado
 * corresponda ao JSON efetivamente produzido pela aplicação.
 */
class ContratoOpenApiIT extends ApiTestSupport {
    private static final Path ESPECIFICACAO = Path.of("docs", "openapi", "foodnow-pedidos.yaml");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static OpenApiInteractionValidator validador;
    private static Map<String, Schema> schemas;

    @BeforeAll
    static void carregarEspecificacao() {
        String uri = ESPECIFICACAO.toAbsolutePath().toUri().toString();
        var leitura = new OpenAPIV3Parser().readLocation(uri, null, null);
        assertThat(leitura.getMessages()).as("Especificação OpenAPI deve ser válida").isEmpty();
        schemas = leitura.getOpenAPI().getComponents().getSchemas();
        validador = OpenApiInteractionValidator.createForSpecificationUrl(uri).build();
    }

    @Test
    void operacoesDePedidoDevemSeguirContratoDocumentado() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);

        Response criado = chamar("POST", "/pedidos", pedido(cenario), 201);
        long pedidoId = criado.jsonPath().getLong("id");
        assertarCamposDocumentados(criado);

        assertarCamposDocumentados(chamar("GET", "/pedidos/" + pedidoId, null, 200));
        assertarCamposDocumentados(chamar("POST", "/pedidos/" + pedidoId + "/itens",
                Map.of("produtoId", cenario.produtoId(), "quantidade", 1), 200));
        assertarCamposDocumentados(chamar("POST", "/pedidos/" + pedidoId + "/confirmar", null, 200));
    }

    @Test
    void operacoesComObservacaoDevemSeguirContratoDocumentado() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        Map<String, Object> corpo = new HashMap<>(pedido(cenario));
        corpo.put("observacao", "a".repeat(500));

        Response criado = chamar("POST", "/pedidos", corpo, 201);
        long pedidoId = criado.jsonPath().getLong("id");
        assertarCamposDocumentados(criado);
        assertarCamposDocumentados(chamar("GET", "/pedidos/" + pedidoId, null, 200));
        assertarCamposDocumentados(chamar("POST", "/pedidos/" + pedidoId + "/itens",
                Map.of("produtoId", cenario.produtoId(), "quantidade", 1), 200));
        assertarCamposDocumentados(chamar("POST", "/pedidos/" + pedidoId + "/confirmar", null, 200));

        corpo.put("observacao", "   ");
        assertarCamposDocumentados(chamar("POST", "/pedidos", corpo, 201));
        corpo.put("observacao", null);
        assertarCamposDocumentados(chamar("POST", "/pedidos", corpo, 201));
        corpo.put("observacao", "a".repeat(501));
        chamarSemValidarRequisicao("POST", "/pedidos", "application/json", JSON.writeValueAsString(corpo), 400);
    }

    @Test
    void errosDasOperacoesDePedidoDevemSeguirContratoDocumentado() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);

        chamar("GET", "/pedidos/999999", null, 404);
        chamar("POST", "/pedidos/999999/confirmar", null, 404);
        chamar("POST", "/pedidos/" + pedidoId + "/itens", Map.of("produtoId", 999999, "quantidade", 1), 404);
        chamar("POST", "/pedidos/" + pedidoId + "/confirmar", null, 200);
        chamar("POST", "/pedidos/" + pedidoId + "/confirmar", null, 422);

        Map<String, Object> semItens = Map.of("clienteId", cenario.clienteId(),
                "restauranteId", cenario.restauranteId(), "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of());
        chamarSemValidarRequisicao("POST", "/pedidos", "application/json", JSON.writeValueAsString(semItens), 400);
        chamarSemValidarRequisicao("POST", "/pedidos", "application/json", "{\"clienteId\":1", 400);
        chamarSemValidarRequisicao("POST", "/pedidos", "text/plain", "x", 415);
        chamarSemValidarRequisicao("GET", "/pedidos/abc", null, null, 422);
    }

    protected Map<String, Object> pedido(Cenario cenario) {
        return Map.of("clienteId", cenario.clienteId(), "restauranteId", cenario.restauranteId(),
                "enderecoEntregaId", cenario.enderecoId(),
                "itens", List.of(Map.of("produtoId", cenario.produtoId(), "quantidade", 2)));
    }

    /** Executa a chamada e valida requisição e resposta contra a especificação. */
    protected Response chamar(String metodo, String caminho, Map<String, Object> corpo, int statusEsperado) {
        String json = corpo == null ? null : JSON.writeValueAsString(corpo);
        Response resposta = executar(metodo, caminho, json == null ? null : "application/json", json, statusEsperado);
        var requisicao = new SimpleRequest.Builder(metodo, caminho);
        if (json != null) {
            requisicao.withContentType("application/json").withBody(json);
        }
        assertarSemErros(metodo + " " + caminho, validador.validate(requisicao.build(), respostaSimples(resposta)));
        return resposta;
    }

    /** Para requisições propositalmente inválidas: valida apenas a resposta contra a especificação. */
    protected Response chamarSemValidarRequisicao(String metodo, String caminho, String contentType,
                                                  String corpo, int statusEsperado) {
        Response resposta = executar(metodo, caminho, contentType, corpo, statusEsperado);
        assertarSemErros(metodo + " " + caminho, validador.validateResponse(caminho,
                Request.Method.valueOf(metodo), respostaSimples(resposta)));
        return resposta;
    }

    /** Falha se a resposta contiver campos que a especificação não documenta. */
    protected void assertarCamposDocumentados(Response resposta) {
        Map<String, Object> pedido = resposta.jsonPath().getMap("$");
        assertThat(propriedades("PedidoResponse")).as("Campos de PedidoResponse documentados")
                .containsAll(pedido.keySet());
        List<Map<String, Object>> itens = resposta.jsonPath().getList("itens");
        itens.forEach(item -> assertThat(propriedades("ItemResponse")).as("Campos de ItemResponse documentados")
                .containsAll(item.keySet()));
    }

    private Response executar(String metodo, String caminho, String contentType, String corpo, int statusEsperado) {
        RequestSpecification requisicao = given();
        if (contentType != null) {
            requisicao.contentType(contentType);
        }
        if (corpo != null) {
            requisicao.body(corpo);
        }
        Response resposta = requisicao.request(metodo, caminho);
        resposta.then().statusCode(statusEsperado);
        return resposta;
    }

    private SimpleResponse respostaSimples(Response resposta) {
        return SimpleResponse.Builder.status(resposta.statusCode())
                .withHeader("Content-Type", resposta.contentType())
                .withBody(resposta.asString())
                .build();
    }

    private void assertarSemErros(String operacao, ValidationReport relatorio) {
        assertThat(relatorio.getMessages())
                .as("%s deve respeitar a especificação OpenAPI", operacao)
                .filteredOn(mensagem -> mensagem.getLevel() == ValidationReport.Level.ERROR)
                .isEmpty();
    }

    private Set<String> propriedades(String schema) {
        return schemas.get(schema).getProperties().keySet();
    }
}
