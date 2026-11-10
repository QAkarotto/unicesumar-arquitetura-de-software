package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Com o serviço de e-mail fora do ar, o fluxo do pedido deve continuar funcionando.
 */
@Import(FalhaEmailIT.EmailForaDoAr.class)
class FalhaEmailIT extends ApiTestSupport {

    @TestConfiguration
    static class EmailForaDoAr {
        @Bean
        @Primary
        FakeEmailClient emailIndisponivel() {
            return new FakeEmailClient() {
                @Override
                public void enviar(Notificacao notificacao) {
                    throw new IllegalStateException("Serviço de e-mail indisponível");
                }

                @Override
                public void enviar(String destinatario, String assunto, String mensagem) {
                    throw new IllegalStateException("Serviço de e-mail indisponível");
                }
            };
        }
    }

    @Test
    void pedidoDeveSerConfirmadoPagoEEntregueMesmoSemEmail() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);

        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));
        given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_OK"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGO"));

        long entregaId = given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(200)
                .extract().jsonPath().getLong("id");
        given().contentType(ContentType.JSON).body(Map.of("status", "EM_ROTA"))
                .patch("/entregas/{id}/status", entregaId).then().statusCode(200)
                .body("status", equalTo("EM_ROTA"));

        assertThat(emailClient.getEnviadas()).isEmpty();
    }
}
