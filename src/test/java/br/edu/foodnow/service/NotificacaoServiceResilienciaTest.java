package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.logistica.PoliticaEntregaAtual;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusPagamento;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceResilienciaTest {
    private static final String EMAIL_DO_CLIENTE = "cliente@foodnow.test";

    private FakeEmailClient emailClient;
    private LocalizacaoService localizacaoService;
    private LogisticaService logisticaService;
    private NotificacaoService service;
    private Pedido pedido;

    private ch.qos.logback.classic.Logger logger;
    private ListAppender<ILoggingEvent> logs;

    @BeforeEach
    void prepararCenario() {
        emailClient = mock(FakeEmailClient.class);
        localizacaoService = mock(LocalizacaoService.class);
        logisticaService = mock(LogisticaService.class);
        service = new NotificacaoService(emailClient, localizacaoService, logisticaService);

        Endereco endereco = new Endereco("Rua do Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.095, -50.160));
        pedido = new Pedido(new Cliente("Cliente Teste", EMAIL_DO_CLIENTE),
                new Restaurante("Cantina", 25, endereco), endereco);
        ReflectionTestUtils.setField(pedido, "id", 42L);

        when(localizacaoService.calcularRota(any(), any())).thenReturn(new Rota(3.2, 25, "CENTRAL"));
        when(logisticaService.resumirPedido(pedido)).thenReturn(new PoliticaEntregaAtual.ResumoPedido("CENTRAL", 30));

        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(NotificacaoService.class);
        logger.setLevel(Level.DEBUG);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void liberarLogs() {
        logger.detachAppender(logs);
        logger.setLevel(null);
    }

    @Test
    void naoDevePropagarFalhaDoEmailAoNotificarPagamentoAprovado() {
        doThrow(new FalhaDoProvedorException()).when(emailClient).enviar(any(Notificacao.class));

        assertThatCode(() -> service.notificarPagamento(pedido, StatusPagamento.APROVADO))
                .doesNotThrowAnyException();

        // Sem nova tentativa: repetir o envio poderia duplicar o e-mail se o primeiro tivesse chegado.
        verify(emailClient, times(1)).enviar(any(Notificacao.class));
        assertThat(mensagensDeErro()).singleElement().satisfies(mensagem ->
                assertThat(mensagem).contains("operacao=notificarPagamento", "pedidoId=42",
                        "tipo=FalhaDoProvedorException"));
    }

    @Test
    void naoDeveRegistrarDadosSensiveisNoLogDaFalha() {
        doThrow(new FalhaDoProvedorException()).when(emailClient).enviar(any(Notificacao.class));

        service.notificarPagamento(pedido, StatusPagamento.APROVADO);

        assertThat(logs.list).isNotEmpty().allSatisfy(evento -> {
            String mensagem = evento.getFormattedMessage();
            assertThat(mensagem).doesNotContain(EMAIL_DO_CLIENTE, "-25.095", "-50.16", "Rua do Teste");
            assertThat(evento.getThrowableProxy()).isNull();
        });
    }

    @Test
    void naoDevePropagarFalhaDaRotaAoNotificarPagamentoRejeitado() {
        when(localizacaoService.calcularRota(any(), any())).thenThrow(new FalhaDoProvedorException());

        assertThatCode(() -> service.notificarPagamento(pedido, StatusPagamento.REJEITADO))
                .doesNotThrowAnyException();

        verify(emailClient, never()).enviar(any(Notificacao.class));
        assertThat(mensagensDeErro()).singleElement().satisfies(mensagem ->
                assertThat(mensagem).contains("operacao=notificarPagamento", "pedidoId=42"));
    }

    @Test
    void naoDevePropagarFalhaDoEmailAoNotificarConfirmacaoEInicioDeEntrega() {
        doThrow(new FalhaDoProvedorException()).when(emailClient).enviar(any(String.class), any(String.class), any(String.class));

        assertThatCode(() -> {
            service.notificarConfirmacao(pedido);
            service.notificarInicioEntrega(pedido);
        }).doesNotThrowAnyException();

        assertThat(mensagensDeErro()).hasSize(2);
        assertThat(mensagensDeErro().get(0)).contains("operacao=notificarConfirmacao", "pedidoId=42");
        assertThat(mensagensDeErro().get(1)).contains("operacao=notificarInicioEntrega", "pedidoId=42");
    }

    @Test
    void deveManterOEnvioNormalSemRegistrarErroQuandoNaoHaFalha() {
        service.notificarPagamento(pedido, StatusPagamento.APROVADO);

        verify(emailClient, times(1)).enviar(any(Notificacao.class));
        assertThat(mensagensDeErro()).isEmpty();
    }

    private List<String> mensagensDeErro() {
        return logs.list.stream()
                .filter(evento -> evento.getLevel() == Level.ERROR)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private static class FalhaDoProvedorException extends RuntimeException {
        FalhaDoProvedorException() {
            super("timeout do provedor para " + EMAIL_DO_CLIENTE);
        }
    }
}
