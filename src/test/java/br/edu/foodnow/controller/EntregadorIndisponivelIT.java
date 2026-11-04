package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeCourierClient;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@Import(EntregadorIndisponivelIT.ProvedorForaDoAr.class)
@TestPropertySource(properties = {"foodnow.courier.timeout=200ms", "foodnow.courier.intervalo=0ms"})
class EntregadorIndisponivelIT extends ApiTestSupport {

    @Test
    void deveAprovarPagamentoECriarEntregaSemEntregadorQuandoOProvedorFalha() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);

        given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_APROVADO"))
                .post("/pagamentos").then().statusCode(201)
                .body("status", equalTo("APROVADO"));
        given().get("/pedidos/{id}", pedidoId).then().statusCode(200)
                .body("status", equalTo("PAGO"));
        given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(200)
                .body("status", equalTo("AGUARDANDO_ENTREGADOR"))
                .body("statusDespachoExterno", equalTo("NO_COURIER_AVAILABLE"))
                .body("codigoEntregadorExterno", nullValue());
    }

    @TestConfiguration
    static class ProvedorForaDoAr {
        @Bean
        @Primary
        FakeCourierClient courierForaDoAr() {
            return new FakeCourierClient() {
                @Override
                public CourierDispatch solicitarEntregador(CourierRequest request) {
                    throw new IllegalStateException("connection refused");
                }
            };
        }
    }
}
