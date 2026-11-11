package br.edu.foodnow.event;

import br.edu.foodnow.FoodNowApplication;
import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.repository.ClienteRepository;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.repository.ProdutoRepository;
import br.edu.foodnow.repository.RestauranteRepository;
import br.edu.foodnow.service.PedidoService;
import br.edu.foodnow.service.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = FoodNowApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.datasource.url=jdbc:h2:mem:foodnow-eventos-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@RecordApplicationEvents
@ExtendWith(OutputCaptureExtension.class)
class PedidoConfirmadoIT {
    @Autowired
    private PedidoService pedidoService;
    @Autowired
    private PedidoRepository pedidos;
    @Autowired
    private ClienteRepository clientes;
    @Autowired
    private RestauranteRepository restaurantes;
    @Autowired
    private ProdutoRepository produtos;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ApplicationEvents eventos;
    @MockitoSpyBean
    private FakeEmailClient emailClient;

    @BeforeEach
    void limparEmails() {
        emailClient.limpar();
    }

    @Test
    void devePublicarDadosDaConfirmacaoENotificarEmOutraThreadSomenteAposCommit() {
        Long pedidoId = criarPedido(true);
        Thread threadPrincipal = Thread.currentThread();
        AtomicReference<Thread> threadConsumidor = new AtomicReference<>();
        AtomicReference<StatusPedido> statusNoBanco = new AtomicReference<>();
        CountDownLatch processado = new CountDownLatch(1);
        doAnswer(invocacao -> {
            threadConsumidor.set(Thread.currentThread());
            statusNoBanco.set(pedidos.findById(pedidoId).orElseThrow().getStatus());
            invocacao.callRealMethod();
            processado.countDown();
            return null;
        }).when(emailClient).enviar(anyString(), anyString(), anyString());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            pedidoService.confirmar(pedidoId);
            assertThat(eventos.stream(PedidoConfirmado.class)).containsExactly(
                    new PedidoConfirmado(pedidoId, "ana@foodnow.test", "CENTRAL", 21));
            verify(emailClient, never()).enviar(anyString(), anyString(), anyString());
        });

        aguardar(processado);
        assertThat(threadConsumidor.get()).isNotSameAs(threadPrincipal);
        assertThat(threadConsumidor.get().getName()).startsWith("foodnow-eventos-");
        assertThat(statusNoBanco.get()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(emailClient.getEnviadas()).singleElement().satisfies(notificacao -> {
            assertThat(notificacao.getDestinatario()).isEqualTo("ana@foodnow.test");
            assertThat(notificacao.getAssunto()).isEqualTo("Pedido confirmado");
            assertThat(notificacao.getMensagem()).isEqualTo("O pedido " + pedidoId
                    + " foi confirmado para a região CENTRAL. Estimativa interna: 21 minutos.");
        });
    }

    @Test
    void consumidorLentoNaoDeveBloquearRetornoDaConfirmacao() {
        Long pedidoId = criarPedido(true);
        CountDownLatch iniciado = new CountDownLatch(1);
        CountDownLatch liberarEnvio = new CountDownLatch(1);
        CountDownLatch concluido = new CountDownLatch(1);
        doAnswer(invocacao -> {
            iniciado.countDown();
            try {
                if (!liberarEnvio.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Envio não foi liberado pelo teste");
                }
                return invocacao.callRealMethod();
            } finally {
                concluido.countDown();
            }
        }).when(emailClient).enviar(anyString(), anyString(), anyString());

        try {
            Pedido confirmado = pedidoService.confirmar(pedidoId);
            aguardar(iniciado);
            assertThat(confirmado.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
            assertThat(pedidos.findById(pedidoId).orElseThrow().getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
            assertThat(concluido.getCount()).isEqualTo(1);
            assertThat(emailClient.getEnviadas()).isEmpty();
        } finally {
            liberarEnvio.countDown();
            aguardar(concluido);
        }
        assertThat(emailClient.getEnviadas()).hasSize(1);
    }

    @Test
    void falhaNoEmailDeveSerRegistradaSemDesfazerConfirmacao(CapturedOutput output) {
        Long pedidoId = criarPedido(true);
        doThrow(new IllegalStateException("Falha simulada no envio de e-mail"))
                .when(emailClient).enviar(anyString(), anyString(), anyString());

        Pedido confirmado = pedidoService.confirmar(pedidoId);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(output.getAll()).contains(
                        "Falha ao enviar notificação de confirmação do pedido " + pedidoId,
                        "Falha simulada no envio de e-mail"));
        assertThat(confirmado.getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(pedidos.findById(pedidoId).orElseThrow().getStatus()).isEqualTo(StatusPedido.CONFIRMADO);
        assertThat(emailClient.getEnviadas()).isEmpty();
    }

    @Test
    void rollbackDeveDescartarNotificacaoMesmoComEventoPublicado() {
        Long pedidoId = criarPedido(true);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            pedidoService.confirmar(pedidoId);
            assertThat(eventos.stream(PedidoConfirmado.class)).hasSize(1);
            status.setRollbackOnly();
        });

        assertThat(pedidos.findById(pedidoId).orElseThrow().getStatus()).isEqualTo(StatusPedido.CRIADO);
        verify(emailClient, after(250).never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void confirmacaoInvalidaNaoDevePublicarEvento() {
        Long pedidoId = criarPedido(false);

        assertThatThrownBy(() -> pedidoService.confirmar(pedidoId))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Pedido deve possuir ao menos um item");

        assertThat(eventos.stream(PedidoConfirmado.class)).isEmpty();
        assertThat(pedidos.findById(pedidoId).orElseThrow().getStatus()).isEqualTo(StatusPedido.CRIADO);
        verify(emailClient, never()).enviar(anyString(), anyString(), anyString());
    }

    private Long criarPedido(boolean comItens) {
        Endereco destino = new Endereco("Destino", "2", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.095, -50.160));
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(destino);
        clientes.save(cliente);
        Endereco origem = new Endereco("Origem", "1", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.100, -50.150));
        Restaurante restaurante = restaurantes.save(new Restaurante("Cantina", 25, origem));
        Pedido pedido = new Pedido(cliente, restaurante, destino);
        if (comItens) {
            Produto produto = produtos.save(new Produto("Pizza", new BigDecimal("30.00"), true, restaurante));
            pedido.adicionarItem(produto, 2);
        }
        return pedidos.save(pedido).getId();
    }

    private void aguardar(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Espera pelo consumidor interrompida", excecao);
        }
    }
}
