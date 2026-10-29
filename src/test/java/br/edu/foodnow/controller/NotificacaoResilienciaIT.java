package br.edu.foodnow.controller;

import br.edu.foodnow.integration.EmailIndisponivelException;
import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import io.restassured.http.ContentType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Import(NotificacaoResilienciaIT.Configuracao.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:foodnow-email-falha;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
class NotificacaoResilienciaIT extends ApiTestSupport {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void devePersistirOperacoesMesmoComEmailIndisponivel(boolean pagamentoAprovado) {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);

        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));

        long pagamentoId = given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX",
                        "token", pagamentoAprovado ? "TOKEN_OK" : "REJEITADO"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo(pagamentoAprovado ? "APROVADO" : "REJEITADO"))
                .extract().jsonPath().getLong("id");
        given().get("/pagamentos/{id}", pagamentoId).then().statusCode(200)
                .body("status", equalTo(pagamentoAprovado ? "APROVADO" : "REJEITADO"));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo(pagamentoAprovado ? "PAGO" : "PAGAMENTO_REJEITADO"));

        if (pagamentoAprovado) {
            long entregaId = given().get("/entregas/pedido/{id}", pedidoId).then().statusCode(200)
                    .extract().jsonPath().getLong("id");
            given().contentType(ContentType.JSON).body(Map.of("status", "EM_ROTA"))
                    .patch("/entregas/{id}/status", entregaId).then().statusCode(200)
                    .body("status", equalTo("EM_ROTA"));
            given().get("/entregas/{id}", entregaId).then().statusCode(200)
                    .body("status", equalTo("EM_ROTA"));
            given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                    .body("status", equalTo("EM_ENTREGA"));
        } else {
            given().get("/entregas/pedido/{id}", pedidoId).then().statusCode(404);
        }

        ClienteIndisponivel cliente = (ClienteIndisponivel) emailClient;
        assertThat(cliente.tentativas.get()).isEqualTo(pagamentoAprovado ? 3 : 2);
        assertThat(cliente.getEnviadas()).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracao {
        @Bean
        @Primary
        ClienteIndisponivel emailIndisponivel() {
            return new ClienteIndisponivel();
        }
    }

    static class ClienteIndisponivel extends FakeEmailClient {
        final AtomicInteger tentativas = new AtomicInteger();

        @Override
        public void enviar(Notificacao notificacao) {
            tentativas.incrementAndGet();
            throw new EmailIndisponivelException("Falha simulada no teste");
        }

        @Override
        public void limpar() {
            super.limpar();
            tentativas.set(0);
        }
    }
}
