package br.edu.foodnow.controller;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.service.NotificacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

class PedidoConfirmacaoAssincronaIT extends ApiTestSupport {

    @MockitoBean
    private NotificacaoService notificacoes;

    @Test
    void erroNoEnvioNaoDesfazPedidoConfirmado() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        doThrow(new IllegalStateException("Email indisponível"))
                .when(notificacoes).notificarConfirmacao(any(Pedido.class));

        given().post("/pedidos/{id}/confirmar", pedidoId)
                .then().statusCode(200).body("status", equalTo("CONFIRMADO"));
        given().get("/pedidos/{id}", pedidoId)
                .then().statusCode(200).body("status", equalTo("CONFIRMADO"));
    }
}
