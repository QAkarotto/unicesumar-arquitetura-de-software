package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakePaymentGateway;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Import(GatewayPagamentoInstavelIT.ProvedorInstavel.class)
@ExtendWith(OutputCaptureExtension.class)
class GatewayPagamentoInstavelIT extends ApiTestSupport {
    private static final String TOKEN = "TOKEN_SECRETO_APROVADO";

    @Autowired
    private GatewayInstavel gateway;

    @BeforeEach
    void reiniciarProvedor() {
        gateway.reiniciar();
    }

    @Test
    void respostaPerdidaDepoisDaCobrancaDeveReconciliarSemCobrarDeNovo(CapturedOutput saida) {
        long pedidoId = pedidoConfirmado();
        gateway.roteiro(Falha.DEPOIS_DE_COBRAR);

        pagar(pedidoId).then().statusCode(201)
                .body("status", equalTo("APROVADO"))
                .body("codigoExterno", equalTo("FN-" + pedidoId));

        assertThat(gateway.cobrancas.get()).isEqualTo(1);
        assertThat(gateway.chamadas.get()).isEqualTo(1);
        given().get("/pedidos/{id}", pedidoId).then().body("status", equalTo("PAGO"));
        assertThat(saida.getOut())
                .contains("operacao=processar_pagamento pedido=" + pedidoId + " tentativa=1/3 falha=ProvedorTimeoutException")
                .contains("pedido=" + pedidoId + " resultado=reconciliado transacao=FN-" + pedidoId + " status=AUTHORIZED")
                .doesNotContain(TOKEN);
    }

    @Test
    void indisponibilidadeTemporariaDeveSerSuperadaComNovaTentativa(CapturedOutput saida) {
        long pedidoId = pedidoConfirmado();
        gateway.roteiro(Falha.ANTES_DE_COBRAR, Falha.ANTES_DE_COBRAR);

        pagar(pedidoId).then().statusCode(201).body("status", equalTo("APROVADO"));

        assertThat(gateway.chamadas.get()).isEqualTo(3);
        assertThat(gateway.cobrancas.get()).isEqualTo(1);
        assertThat(saida.getOut())
                .contains("pedido=" + pedidoId + " tentativa=2/3 falha=ProvedorIndisponivelException")
                .contains("pedido=" + pedidoId + " tentativa=3/3 resultado=recuperado status=AUTHORIZED")
                .doesNotContain(TOKEN);
    }

    @Test
    void indisponibilidadeContinuaDeveResponder503EManterOPedidoPagavel(CapturedOutput saida) {
        long pedidoId = pedidoConfirmado();
        gateway.roteiro(Falha.ANTES_DE_COBRAR, Falha.ANTES_DE_COBRAR, Falha.ANTES_DE_COBRAR);

        pagar(pedidoId).then().statusCode(503)
                .body("erro", equalTo("PAGAMENTO_INDISPONIVEL"));

        assertThat(gateway.chamadas.get()).isEqualTo(3);
        assertThat(gateway.cobrancas.get()).isZero();
        given().get("/pedidos/{id}", pedidoId).then().body("status", equalTo("CONFIRMADO"));
        assertThat(saida.getOut())
                .contains("pedido=" + pedidoId + " resultado=tentativas_esgotadas tentativas=3")
                .doesNotContain(TOKEN);

        pagar(pedidoId).then().statusCode(201).body("status", equalTo("APROVADO"));
        assertThat(gateway.cobrancas.get()).isEqualTo(1);
    }

    @Test
    void falhaNaConsultaNaoDeveRepetirACobranca(CapturedOutput saida) {
        long pedidoId = pedidoConfirmado();
        gateway.roteiro(Falha.ANTES_DE_COBRAR);
        gateway.consultaForaDoAr = true;

        pagar(pedidoId).then().statusCode(503)
                .body("erro", equalTo("PAGAMENTO_INDISPONIVEL"));

        assertThat(gateway.chamadas.get()).isEqualTo(1);
        assertThat(saida.getOut())
                .contains("operacao=consultar_pagamento pedido=" + pedidoId + " transacao=FN-" + pedidoId
                        + " falha=ProvedorIndisponivelException resultado=interrompido_estado_desconhecido")
                .doesNotContain(TOKEN);
    }

    private long pedidoConfirmado() {
        long pedidoId = criarPedido(criarCenario(true, 25, -25.095, -50.160));
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);
        return pedidoId;
    }

    private io.restassured.response.Response pagar(long pedidoId) {
        return given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", TOKEN))
                .post("/pagamentos");
    }

    enum Falha { ANTES_DE_COBRAR, DEPOIS_DE_COBRAR }

    static class ProvedorIndisponivelException extends RuntimeException {
        ProvedorIndisponivelException() {
            super("connection refused token=" + TOKEN);
        }
    }

    static class ProvedorTimeoutException extends RuntimeException {
        ProvedorTimeoutException() {
            super("read timed out token=" + TOKEN);
        }
    }

    static class GatewayInstavel extends FakePaymentGateway {
        final AtomicInteger chamadas = new AtomicInteger();
        final AtomicInteger cobrancas = new AtomicInteger();
        private final Deque<Falha> roteiro = new ArrayDeque<>();
        volatile boolean consultaForaDoAr;

        synchronized void roteiro(Falha... falhas) {
            roteiro.addAll(List.of(falhas));
        }

        synchronized void reiniciar() {
            roteiro.clear();
            chamadas.set(0);
            cobrancas.set(0);
            consultaForaDoAr = false;
        }

        @Override
        public GatewayResult processarPagamento(GatewayRequest request) {
            chamadas.incrementAndGet();
            Falha falha;
            synchronized (this) {
                falha = roteiro.poll();
            }
            if (falha == Falha.ANTES_DE_COBRAR) {
                throw new ProvedorIndisponivelException();
            }
            GatewayResult resultado = super.processarPagamento(request);
            cobrancas.incrementAndGet();
            if (falha == Falha.DEPOIS_DE_COBRAR) {
                throw new ProvedorTimeoutException();
            }
            return resultado;
        }

        @Override
        public GatewayResult consultarPagamento(String codigo) {
            if (consultaForaDoAr) {
                throw new ProvedorIndisponivelException();
            }
            return super.consultarPagamento(codigo);
        }
    }

    @TestConfiguration
    static class ProvedorInstavel {
        @Bean
        @Primary
        GatewayInstavel gatewayInstavel() {
            return new GatewayInstavel();
        }
    }
}
