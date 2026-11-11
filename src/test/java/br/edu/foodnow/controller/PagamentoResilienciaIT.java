
package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
class PagamentoResilienciaIT extends ApiTestSupport {

    @MockitoBean
    private FakeEmailClient emailClientMock;

    @Test
    void devePreservarPagamentoQuandoNotificacaoFalhar(
            CapturedOutput output) {

        // Arrange: preparar um pedido válido.
        Cenario cenario = criarCenario(
                true, 25, -25.095, -50.160
        );

        long pedidoId = criarPedido(cenario);

        given()
                .post("/pedidos/{id}/confirmar", pedidoId)
                .then()
                .statusCode(200);

        // Simular falha no envio da notificação de pagamento.
        doThrow(new IllegalStateException(
                "Falha simulada no servico de e-mail"
        )).when(emailClientMock).enviar(any(Notificacao.class));

        // Act: processar o pagamento.
        long pagamentoId = given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "pedidoId", pedidoId,
                        "forma", "PIX",
                        "token", "TOKEN_APROVADO"
                ))
                .post("/pagamentos")
                .then()
                .statusCode(201)
                .body("status", equalTo("APROVADO"))
                .extract()
                .jsonPath()
                .getLong("id");

        // Assert: o pagamento continua salvo e aprovado.
        given()
                .get("/pagamentos/{id}", pagamentoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("APROVADO"));

        // O pedido também deve permanecer pago.
        given()
                .get("/pedidos/{id}", pedidoId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PAGO"));

        // A tentativa de enviar a notificação ocorreu.
        verify(emailClientMock, times(1))
                .enviar(any(Notificacao.class));

        // Verificar os registros de diagnóstico.
        assertThat(output.getAll())
                .contains("Falha no envio da notificacao de pagamento")
                .contains("pedidoId=" + pedidoId)
                .contains("pagamentoId=" + pagamentoId)
                .contains("tipoFalha=IllegalStateException")
                .contains("resultado=NAO_ENVIADA");
    }
}
