package br.edu.foodnow.controller;

import br.edu.foodnow.event.PedidoConfirmadoEvent;
import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.service.LocalizacaoService;
import br.edu.foodnow.service.NotificacaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Import(PedidoConfirmacaoFalhaIT.ConfiguracaoComFalha.class)
class PedidoConfirmacaoFalhaIT extends ApiTestSupport {
    private static volatile CountDownLatch consumidorExecutado = new CountDownLatch(1);

    @BeforeEach
    void prepararSinalizador() {
        consumidorExecutado = new CountDownLatch(1);
    }

    @Test
    void falhaNaNotificacaoAssincronaNaoDesfazConfirmacaoDoPedido() throws InterruptedException {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);

        given().post("/pedidos/{id}/confirmar", pedidoId)
                .then().statusCode(200).body("status", equalTo("CONFIRMADO"));

        assertThat(consumidorExecutado.await(5, TimeUnit.SECONDS))
                .as("o consumidor deve executar e simular a falha secundária")
                .isTrue();

        given().get("/pedidos/{id}", pedidoId)
                .then().statusCode(200).body("status", equalTo("CONFIRMADO"));
    }

    @TestConfiguration
    static class ConfiguracaoComFalha {
        @Bean
        @Primary
        NotificacaoService notificacaoServiceComFalha(FakeEmailClient emailClient,
                                                       LocalizacaoService localizacaoService) {
            return new NotificacaoService(emailClient, localizacaoService) {
                @Override
                public void notificarConfirmacao(PedidoConfirmadoEvent evento) {
                    consumidorExecutado.countDown();
                    throw new IllegalStateException("Falha simulada no envio da notificação");
                }
            };
        }
    }
}
