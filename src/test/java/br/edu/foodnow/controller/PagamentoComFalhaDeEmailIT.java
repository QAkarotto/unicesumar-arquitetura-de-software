package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Simula a indisponibilidade do provedor de e-mail no momento em que o pagamento é aprovado.
 * Sem o tratamento em NotificacaoService, a exceção atravessava PagamentoService.processar
 * (transacional): a transação era desfeita, o pedido continuava CONFIRMADO, nenhuma entrega
 * era criada e a API respondia 500, mesmo com o pagamento já autorizado no provedor.
 */
class PagamentoComFalhaDeEmailIT extends ApiTestSupport {

    @MockitoSpyBean
    private FakeEmailClient emailSpy;

    @Test
    void devePreservarPagamentoEEntregaQuandoOEmailDeAprovacaoFalha() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);

        doThrow(new RuntimeException("timeout do provedor de e-mail"))
                .when(emailSpy).enviar(any(Notificacao.class));

        given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_APROVADO"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));

        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGO"));
        given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(200)
                .body("status", equalTo("AGUARDANDO_ENTREGADOR"));
        assertThat(emailClient.getEnviadas()).extracting("assunto")
                .containsExactly("Pedido confirmado");
    }
}
