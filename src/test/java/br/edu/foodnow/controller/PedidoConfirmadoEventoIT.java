package br.edu.foodnow.controller;

import br.edu.foodnow.event.PedidoConfirmado;
import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.service.PedidoService;
import br.edu.foodnow.service.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

@Import(PedidoConfirmadoEventoIT.Configuracao.class)
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:foodnow-eventos;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@ExtendWith(OutputCaptureExtension.class)
class PedidoConfirmadoEventoIT extends ApiTestSupport {
    @Autowired private PedidoService pedidos;
    @Autowired private ApplicationEventPublisher publicador;
    @Autowired private PlatformTransactionManager transacoes;
    @Autowired private ThreadPoolTaskExecutor notificacoesExecutor;
    @Autowired private EventosRecebidos eventos;

    @BeforeEach
    void prepararConsumidor() throws Exception {
        aguardarExecutor();
        cliente().preparar();
        eventos.recebidos.clear();
    }

    @Test
    void devePublicarEEnviarAposCommitEmOutraThread() throws Exception {
        long id = novoPedido();
        Thread produtora = Thread.currentThread();
        try {
            pedidos.confirmar(id);
            assertThat(cliente().entrada.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(eventos.recebidos).singleElement().satisfies(evento -> {
                assertThat(evento.pedidoId()).isEqualTo(id);
                assertThat(evento.destinatario()).isEqualTo("cliente@foodnow.test");
                assertThat(evento.regiao()).isEqualTo("CENTRAL");
                assertThat(evento.tempoEstimadoMinutos()).isPositive();
            });
            assertThat(cliente().threadConsumidora).isNotSameAs(produtora);
            assertThat(cliente().threadConsumidora.getName()).startsWith("notificacao-");
            assertThat(cliente().transacaoAtiva).isFalse();
            assertThat(cliente().getEnviadas()).isEmpty();
            // O consumidor permanece bloqueado; a confirmação já retornou e está persistida.
            given().get("/pedidos/{id}", id).then().statusCode(200)
                    .body("status", equalTo("CONFIRMADO"));
        } finally {
            cliente().liberar.countDown();
            aguardarExecutor();
        }
        assertThat(cliente().getEnviadas()).singleElement().satisfies(notificacao -> {
            PedidoConfirmado evento = eventos.recebidos.getFirst();
            assertThat(notificacao.getDestinatario()).isEqualTo(evento.destinatario());
            assertThat(notificacao.getAssunto()).isEqualTo("Pedido confirmado");
            assertThat(notificacao.getMensagem()).isEqualTo("O pedido " + id
                    + " foi confirmado para a região " + evento.regiao()
                    + ". Estimativa interna: " + evento.tempoEstimadoMinutos() + " minutos.");
        });
        assertThat(cliente().tentativas.get()).isEqualTo(1);
    }

    @Test
    void falhaDoConsumidorNaoDeveDesfazerConfirmacao(CapturedOutput saida) throws Exception {
        long id = novoPedido();
        cliente().falhar = true;
        try {
            pedidos.confirmar(id);
            assertThat(cliente().entrada.await(10, TimeUnit.SECONDS)).isTrue();
            given().get("/pedidos/{id}", id).then().statusCode(200)
                    .body("status", equalTo("CONFIRMADO"));
        } finally {
            cliente().liberar.countDown();
            aguardarExecutor();
        }
        assertThat(cliente().tentativas.get()).isEqualTo(1);
        assertThat(cliente().getEnviadas()).isEmpty();
        assertThat(saida.getAll()).contains("Falha na notificação do pedido " + id,
                "IllegalStateException");
        given().get("/pedidos/{id}", id).then().statusCode(200)
                .body("status", equalTo("CONFIRMADO"));
    }

    @Test
    void rollbackNaoDeveExecutarConsumidor() throws Exception {
        long id = novoPedido();
        new TransactionTemplate(transacoes).executeWithoutResult(status -> {
            pedidos.confirmar(id);
            assertThat(eventos.recebidos).hasSize(1);
            assertThat(cliente().tentativas.get()).isZero();
            status.setRollbackOnly();
        });
        aguardarExecutor();
        assertThat(cliente().tentativas.get()).isZero();
        given().get("/pedidos/{id}", id).then().statusCode(200)
                .body("status", equalTo("CRIADO"));
    }

    @Test
    void consumidorSoDeveSerAgendadoDepoisDoCommit() throws Exception {
        long id = novoPedido();
        try {
            new TransactionTemplate(transacoes).executeWithoutResult(status -> {
                pedidos.confirmar(id);
                assertThat(eventos.recebidos).hasSize(1);
                assertThat(cliente().tentativas.get()).isZero();
            });
            assertThat(cliente().entrada.await(10, TimeUnit.SECONDS)).isTrue();
        } finally {
            cliente().liberar.countDown();
            aguardarExecutor();
        }
        assertThat(cliente().getEnviadas()).hasSize(1);
    }

    @Test
    void eventoSemTransacaoNaoDeveSerIgnorado() throws Exception {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        try {
            publicador.publishEvent(new PedidoConfirmado(123L, "cliente@foodnow.test", "CENTRAL", 21));
            assertThat(cliente().entrada.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(cliente().threadConsumidora).isNotSameAs(Thread.currentThread());
        } finally {
            cliente().liberar.countDown();
            aguardarExecutor();
        }
        assertThat(cliente().getEnviadas()).hasSize(1);
    }

    @Test
    void confirmacaoRejeitadaNaoDevePublicarOutroEvento() throws Exception {
        long id = novoPedido();
        cliente().liberar.countDown();
        pedidos.confirmar(id);
        aguardarExecutor();
        eventos.recebidos.clear();

        assertThatThrownBy(() -> pedidos.confirmar(id)).isInstanceOf(RegraNegocioException.class);

        aguardarExecutor();
        assertThat(eventos.recebidos).isEmpty();
        assertThat(cliente().tentativas.get()).isEqualTo(1);
    }

    private long novoPedido() {
        return criarPedido(criarCenario(true, 25, -25.095, -50.160));
    }

    private void aguardarExecutor() throws Exception {
        // Uma barreira na fila de uma única thread observa a conclusão das tarefas anteriores.
        notificacoesExecutor.submit(() -> { }).get(10, TimeUnit.SECONDS);
    }

    private ClienteControlado cliente() {
        return (ClienteControlado) emailClient;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracao {
        @Bean @Primary
        ClienteControlado clienteControlado() { return new ClienteControlado(); }

        @Bean
        EventosRecebidos eventosRecebidos() { return new EventosRecebidos(); }
    }

    static class EventosRecebidos {
        final List<PedidoConfirmado> recebidos = new CopyOnWriteArrayList<>();

        @EventListener
        public void registrar(PedidoConfirmado evento) { recebidos.add(evento); }
    }

    static class ClienteControlado extends FakeEmailClient {
        final AtomicInteger tentativas = new AtomicInteger();
        volatile CountDownLatch entrada = new CountDownLatch(1);
        volatile CountDownLatch liberar = new CountDownLatch(1);
        volatile Thread threadConsumidora;
        volatile boolean transacaoAtiva;
        volatile boolean falhar;

        void preparar() {
            tentativas.set(0);
            entrada = new CountDownLatch(1);
            liberar = new CountDownLatch(1);
            falhar = false;
            threadConsumidora = null;
        }

        @Override
        public void enviar(String destinatario, String assunto, String mensagem) {
            tentativas.incrementAndGet();
            threadConsumidora = Thread.currentThread();
            transacaoAtiva = TransactionSynchronizationManager.isActualTransactionActive();
            entrada.countDown();
            try {
                if (!liberar.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Consumidor não liberado pelo teste");
                }
            } catch (InterruptedException excecao) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(excecao);
            }
            if (falhar) {
                throw new IllegalStateException("Falha simulada no envio");
            }
            super.enviar(destinatario, assunto, mensagem);
        }
    }
}
