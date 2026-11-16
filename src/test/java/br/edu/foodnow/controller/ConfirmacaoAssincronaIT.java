package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class ConfirmacaoAssincronaIT extends ApiTestSupport {
    @MockitoBean
    private FakeEmailClient clienteEmailComFalha;

    @Test
    void falhaAoEnviarNotificacaoNaoDesfazConfirmacaoDoPedido() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        doThrow(new IllegalStateException("E-mail indisponível"))
                .when(clienteEmailComFalha).enviar(anyString(), eq("Pedido confirmado"), anyString());

        given().post("/pedidos/{id}/confirmar", pedidoId)
                .then().statusCode(200).body("status", org.hamcrest.Matchers.equalTo("CONFIRMADO"));

        verify(clienteEmailComFalha, timeout(2_000))
                .enviar(anyString(), eq("Pedido confirmado"), anyString());
        var consulta = given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .extract().jsonPath();
        assertThat(consulta.getString("status")).isEqualTo("CONFIRMADO");
    }
}
