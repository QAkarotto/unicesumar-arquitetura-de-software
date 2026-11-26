package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.PagamentoProcessado;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Reproduz, pela API, falhas no envio do e-mail de pagamento e a entrega duplicada do evento.
 * Os registros de diagnóstico do consumidor são exibidos na saída do teste (nível INFO).
 */
@TestPropertySource(properties = {
        "foodnow.notificacao.pagamento.tentativas=3",
        "foodnow.notificacao.pagamento.espera-inicial=1ms",
        "logging.level.br.edu.foodnow.service.NotificacaoPagamentoListener=INFO"
})
class NotificacaoPagamentoResilienciaIT extends ApiTestSupport {

    @MockitoSpyBean
    private FakeEmailClient emailEspiao;

    @Autowired
    private ApplicationEventPublisher eventos;

    @Autowired
    private TransactionTemplate transacao;

    @Test
    void deveEnviarEmailDePagamentoAposFalhasTransitorias() {
        long pedidoId = pedidoConfirmado();
        doThrow(new IllegalStateException("SMTP indisponível"))
                .doThrow(new IllegalStateException("SMTP indisponível"))
                .doCallRealMethod()
                .when(emailEspiao).enviar(any(Notificacao.class));

        given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));

        verify(emailEspiao, times(3)).enviar(any(Notificacao.class));
        assertThat(emailEspiao.getEnviadas()).extracting("assunto")
                .containsOnlyOnce("Pagamento aprovado");
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200).body("status", equalTo("PAGO"));
    }

    @Test
    void naoDeveNotificarDuasVezesQuandoEventoEhEntregueEmDuplicidade() {
        long pedidoId = pedidoConfirmado();
        long pagamentoId = given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(201)
                .extract().jsonPath().getLong("id");

        transacao.executeWithoutResult(status ->
                eventos.publishEvent(new PagamentoProcessado(pagamentoId, pedidoId, StatusPagamento.APROVADO)));

        assertThat(emailEspiao.getEnviadas()).extracting("assunto")
                .containsOnlyOnce("Pagamento aprovado");
    }

    @Test
    void deveManterPagamentoQuandoEmailFalhaEmTodasAsTentativas() {
        long pedidoId = pedidoConfirmado();
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(emailEspiao).enviar(any(Notificacao.class));

        given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));

        verify(emailEspiao, times(3)).enviar(any(Notificacao.class));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200).body("status", equalTo("PAGO"));
        doCallRealMethod().when(emailEspiao).enviar(any(Notificacao.class));
    }

    private long pedidoConfirmado() {
        long pedidoId = criarPedido(criarCenario(true, 25, -25.095, -50.160));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);
        return pedidoId;
    }

    private Map<String, Object> pagamento(long pedidoId) {
        return Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_OK");
    }
}
