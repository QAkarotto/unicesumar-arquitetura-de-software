package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
class PagamentoNotificacaoResilienciaIT extends ApiTestSupport {

    @MockitoBean
    private FakeEmailClient emailClient;

    @Test
    void falhaNaNotificacaoNaoDesfazPagamentoERegistraDiagnostico(CapturedOutput output) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);

        doThrow(new IllegalStateException("token=segredo-nao-deve-ir-para-o-log"))
                .when(emailClient).enviar(any(Notificacao.class));

        long pagamentoId = given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN-PRIVADO"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"))
                .extract().jsonPath().getLong("id");

        given().get("/pagamentos/{id}", pagamentoId).then().statusCode(200)
                .body("status", equalTo("APROVADO"));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGO"));

        verify(emailClient).enviar(any(Notificacao.class));
        assertThat(output.getAll())
                .contains("Falha na notificação de pagamento")
                .contains("pedidoId=" + pedidoId)
                .contains("pagamentoId=" + pagamentoId)
                .contains("tipoFalha=IllegalStateException")
                .contains("resultado=NAO_ENVIADA")
                .contains("pagamento preservado")
                .doesNotContain("TOKEN-PRIVADO", "segredo-nao-deve-ir-para-o-log");
    }
}
