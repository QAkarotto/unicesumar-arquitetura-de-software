package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakePaymentGateway;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
        "foodnow.pagamento.gateway.timeout=500ms",
        "foodnow.pagamento.gateway.tentativas=3",
        "foodnow.pagamento.gateway.espera-inicial=1ms"
})
class PagamentoIndisponivelIT extends ApiTestSupport {

    @MockitoSpyBean
    private FakePaymentGateway paymentGateway;

    @Test
    void deveAprovarPagamentoQuandoProvedorFalhaUmaVezEDepoisResponde() {
        long pedidoId = pedidoConfirmado();
        doThrow(new IllegalStateException("Connection reset")).doCallRealMethod()
                .when(paymentGateway).processarPagamento(any());

        given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"))
                .body("codigoExterno", equalTo("FN-" + pedidoId));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200).body("status", equalTo("PAGO"));
        verify(paymentGateway, times(2)).processarPagamento(any());
    }

    @Test
    void deveResponder503EManterPedidoConfirmadoQuandoProvedorSegueIndisponivel() {
        long pedidoId = pedidoConfirmado();
        doThrow(new IllegalStateException("Connection refused"))
                .when(paymentGateway).processarPagamento(any());

        given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(503)
                .body("status", equalTo(503))
                .body("erro", equalTo("SERVICO_INDISPONIVEL"))
                .body("mensagem", equalTo("Serviço de pagamento indisponível. Tente novamente mais tarde."));
        verify(paymentGateway, times(3)).processarPagamento(any());
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200).body("status", equalTo("CONFIRMADO"));
        given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(404);

        doCallRealMethod().when(paymentGateway).processarPagamento(any());
        given().contentType(ContentType.JSON).body(pagamento(pedidoId))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));
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
