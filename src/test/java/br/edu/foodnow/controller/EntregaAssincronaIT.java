package br.edu.foodnow.controller;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.StatusEntrega;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.repository.EntregaRepository;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.service.EntregaService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

class EntregaAssincronaIT extends ApiTestSupport {
    @MockitoSpyBean
    private FakeEmailClient emailSimulado;
    @Autowired
    private EntregaRepository entregas;
    @Autowired
    private PedidoRepository pedidos;
    @Autowired
    private EntregaService entregaService;
    @Autowired
    private PlatformTransactionManager transacoes;

    @Test
    void deveEnviarEmailEmOutraThreadComOsEstadosJaConfirmados() throws Exception {
        Preparacao dados = prepararEntrega();
        CountDownLatch enviada = new CountDownLatch(1);
        AtomicReference<String> thread = new AtomicReference<>();
        AtomicReference<StatusEntrega> statusEntrega = new AtomicReference<>();
        AtomicReference<StatusPedido> statusPedido = new AtomicReference<>();

        doAnswer(chamada -> {
            thread.set(Thread.currentThread().getName());
            statusEntrega.set(entregas.findById(dados.entregaId()).orElseThrow().getStatus());
            statusPedido.set(pedidos.findById(dados.pedidoId()).orElseThrow().getStatus());
            chamada.callRealMethod();
            enviada.countDown();
            return null;
        }).when(emailSimulado).enviar(anyString(), eq("Entrega iniciada"), anyString());

        iniciarPelaApi(dados.entregaId());

        assertThat(enviada.await(5, SECONDS)).isTrue();
        assertThat(thread.get()).startsWith("foodnow-evento-");
        assertThat(statusEntrega.get()).isEqualTo(StatusEntrega.EM_ROTA);
        assertThat(statusPedido.get()).isEqualTo(StatusPedido.EM_ENTREGA);
        verify(emailSimulado).enviar("cliente@foodnow.test", "Entrega iniciada",
                "O pedido " + dados.pedidoId() + " saiu para entrega.");
        conferirEstados(dados, "EM_ROTA", "EM_ENTREGA");
    }

    @Test
    void emailLentoComFalhaNaoDeveBloquearNemDesfazerInicioDaEntrega() throws Exception {
        Preparacao dados = prepararEntrega();
        CountDownLatch entrou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        CountDownLatch terminou = new CountDownLatch(1);

        doAnswer(chamada -> {
            entrou.countDown();
            try {
                if (!liberar.await(10, SECONDS)) {
                    throw new IllegalStateException("Tempo limite da simulação");
                }
                throw new IllegalStateException("E-mail indisponível");
            } finally {
                terminou.countDown();
            }
        }).when(emailSimulado).enviar(anyString(), eq("Entrega iniciada"), anyString());

        try {
            iniciarPelaApi(dados.entregaId());
            assertThat(entrou.await(5, SECONDS)).isTrue();
            // A resposta HTTP e as consultas retornam enquanto o e-mail ainda está bloqueado.
            assertThat(terminou.getCount()).isEqualTo(1);
            conferirEstados(dados, "EM_ROTA", "EM_ENTREGA");
        } finally {
            liberar.countDown();
            assertThat(terminou.await(5, SECONDS)).isTrue();
        }

        verify(emailSimulado).enviar("cliente@foodnow.test", "Entrega iniciada",
                "O pedido " + dados.pedidoId() + " saiu para entrega.");
        conferirEstados(dados, "EM_ROTA", "EM_ENTREGA");
    }

    @Test
    void rollbackNaoDeveEnviarEmailNemPersistirInicioDaEntrega() {
        Preparacao dados = prepararEntrega();

        new TransactionTemplate(transacoes).executeWithoutResult(transacao -> {
            entregaService.atualizarStatus(dados.entregaId(), StatusEntrega.EM_ROTA);
            transacao.setRollbackOnly();
        });

        verify(emailSimulado, after(300).never())
                .enviar(anyString(), eq("Entrega iniciada"), anyString());
        conferirEstados(dados, "AGUARDANDO_ENTREGADOR", "PAGO");
    }

    private Preparacao prepararEntrega() {
        Cenario cenario = criarCenario(true, 25, -25.095, -50.160);
        long pedidoId = criarPedido(cenario);
        given().post("/pedidos/{id}/confirmar", pedidoId).then().statusCode(200);
        given().contentType(ContentType.JSON)
                .body(Map.of("pedidoId", pedidoId, "forma", "PIX", "token", "TOKEN_APROVADO"))
                .post("/pagamentos").then().statusCode(201);
        long entregaId = given().get("/entregas/pedido/{pedidoId}", pedidoId).then().statusCode(200)
                .extract().jsonPath().getLong("id");
        return new Preparacao(pedidoId, entregaId);
    }

    private void iniciarPelaApi(long entregaId) {
        given().contentType(ContentType.JSON).body(Map.of("status", "EM_ROTA"))
                .patch("/entregas/{id}/status", entregaId).then().statusCode(200)
                .body("status", equalTo("EM_ROTA"));
    }

    private void conferirEstados(Preparacao dados, String statusEntrega, String statusPedido) {
        given().get("/entregas/{id}", dados.entregaId()).then().statusCode(200)
                .body("status", equalTo(statusEntrega));
        given().get("/pedidos/{id}", dados.pedidoId()).then().statusCode(200)
                .body("status", equalTo(statusPedido));
    }

    private record Preparacao(long pedidoId, long entregaId) {
    }
}
