package br.edu.foodnow.controller;

import br.edu.foodnow.integration.EmailIndisponivelException;
import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:foodnow-observabilidade;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "logging.level.br.edu.foodnow.service.NotificacaoService=INFO"
})
class NotificacaoPagamentoResilienciaIT extends ApiTestSupport {
    @MockitoSpyBean
    private FakeEmailClient clienteEmail;

    @ParameterizedTest
    @CsvSource({
            "APROVADO, SUCESSO", "REJEITADO, SUCESSO",
            "APROVADO, ANTES_DO_ENVIO", "REJEITADO, ANTES_DO_ENVIO",
            "APROVADO, APOS_ACEITACAO", "REJEITADO, APOS_ACEITACAO"
    })
    void devePreservarPagamentoEDiagnosticarEnvioSemDuplicar(String status, String cenarioFalha,
                                                           CapturedOutput output) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);
        if (!"SUCESSO".equals(cenarioFalha)) {
            doAnswer(invocation -> {
                if ("APOS_ACEITACAO".equals(cenarioFalha)) {
                    invocation.callRealMethod();
                }
                throw new EmailIndisponivelException("DADO_SENSIVEL_NAO_REGISTRAR cliente@foodnow.test");
            }).when(clienteEmail).enviar(any(Notificacao.class));
        }
        String token = "APROVADO".equals(status) ? "TOKEN_PRIVADO_TESTE" : "REJEITADO";

        long pagamentoId = given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", token))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo(status)).extract().jsonPath().getLong("id");

        given().get("/pagamentos/{id}", pagamentoId).then().statusCode(200)
                .body("status", equalTo(status));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("APROVADO".equals(status) ? "PAGO" : "PAGAMENTO_REJEITADO"));
        given().get("/entregas/pedido/{id}", pedidoId).then()
                .statusCode("APROVADO".equals(status) ? 200 : 404);
        verify(clienteEmail, times(1)).enviar(any(Notificacao.class));
        assertThat(clienteEmail.getEnviadas().stream()
                .filter(email -> email.getAssunto().equals("Pagamento " + status.toLowerCase())).count())
                .isEqualTo("ANTES_DO_ENVIO".equals(cenarioFalha) ? 0 : 1);
        String registros = output.getOut();
        assertThat(registros).contains("operacao=notificarPagamento pedidoId=" + pedidoId + " status=" + status);
        if ("SUCESSO".equals(cenarioFalha)) {
            assertThat(registros).contains("resultado=ENVIADA tentativas=1").doesNotContain("ENVIO_INCERTO");
        } else {
            assertThat(registros).contains("tipoFalha=EmailIndisponivelException",
                    "resultado=ENVIO_INCERTO tratamento=CONTINUAR_SEM_REENVIO tentativas=1");
        }
        assertThat(registros).doesNotContain("TOKEN_PRIVADO_TESTE", "DADO_SENSIVEL_NAO_REGISTRAR",
                "cliente@foodnow.test", "Região de entrega:");
    }
}
