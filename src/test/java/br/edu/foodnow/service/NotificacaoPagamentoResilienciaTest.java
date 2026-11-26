package br.edu.foodnow.service;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.PagamentoProcessado;
import br.edu.foodnow.repository.PedidoRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reproduz falhas no envio do e-mail de pagamento e verifica o retry limitado, a prevenção de
 * notificações duplicadas e os registros de diagnóstico gerados.
 */
class NotificacaoPagamentoResilienciaTest {
    private static final PagamentoProcessado EVENTO = new PagamentoProcessado(3L, 7L, StatusPagamento.APROVADO);

    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final NotificacaoService notificacaoService = mock(NotificacaoService.class);
    private final Pedido pedido = mock(Pedido.class);
    private final NotificacaoPagamentoListener listener =
            new NotificacaoPagamentoListener(pedidoRepository, notificacaoService, 3, Duration.ofMillis(1));

    private final Logger logger = (Logger) LoggerFactory.getLogger(NotificacaoPagamentoListener.class);
    private final ListAppender<ILoggingEvent> registros = new ListAppender<>();
    private Level nivelOriginal;

    @BeforeEach
    void capturarRegistros() {
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));
        nivelOriginal = logger.getLevel();
        logger.setLevel(Level.INFO);
        registros.start();
        logger.addAppender(registros);
    }

    @AfterEach
    void liberarRegistros() {
        logger.detachAppender(registros);
        logger.setLevel(nivelOriginal);
    }

    @Test
    void deveRepetirEnvioAposFalhasTransitoriasERegistrarCadaTentativa() {
        doThrow(new IllegalStateException("SMTP indisponível"))
                .doThrow(new IllegalStateException("SMTP indisponível"))
                .doNothing()
                .when(notificacaoService).notificarPagamento(pedido, StatusPagamento.APROVADO);

        listener.aoProcessarPagamento(EVENTO);

        verify(notificacaoService, times(3)).notificarPagamento(pedido, StatusPagamento.APROVADO);
        assertThat(mensagens()).containsExactly(
                "WARN operacao=notificacao-pagamento pagamentoId=3 pedidoId=7 tentativa=1/3 "
                        + "falha=IllegalStateException acao=NOVA_TENTATIVA esperaMs=1",
                "WARN operacao=notificacao-pagamento pagamentoId=3 pedidoId=7 tentativa=2/3 "
                        + "falha=IllegalStateException acao=NOVA_TENTATIVA esperaMs=2",
                "INFO operacao=notificacao-pagamento pagamentoId=3 pedidoId=7 statusPagamento=APROVADO "
                        + "tentativas=3 resultado=ENVIADO");
    }

    @Test
    void deveDesistirAposOLimiteSemPropagarAFalha() {
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(notificacaoService).notificarPagamento(any(), any());

        assertThatCode(() -> listener.aoProcessarPagamento(EVENTO)).doesNotThrowAnyException();

        verify(notificacaoService, times(3)).notificarPagamento(pedido, StatusPagamento.APROVADO);
        assertThat(mensagens()).last().isEqualTo(
                "ERROR operacao=notificacao-pagamento pagamentoId=3 pedidoId=7 tentativas=3 "
                        + "falha=IllegalStateException resultado=FALHA_DEFINITIVA");
    }

    @Test
    void deveIgnorarEventoDuplicadoDoMesmoPagamento() {
        listener.aoProcessarPagamento(EVENTO);
        listener.aoProcessarPagamento(EVENTO);

        verify(notificacaoService, times(1)).notificarPagamento(pedido, StatusPagamento.APROVADO);
        assertThat(mensagens()).last().isEqualTo(
                "WARN operacao=notificacao-pagamento pagamentoId=3 pedidoId=7 resultado=DUPLICADO_IGNORADO");
    }

    @Test
    void devePermitirReprocessarEventoAposFalhaDefinitiva() {
        doThrow(new IllegalStateException("SMTP indisponível"))
                .doThrow(new IllegalStateException("SMTP indisponível"))
                .doThrow(new IllegalStateException("SMTP indisponível"))
                .doNothing()
                .when(notificacaoService).notificarPagamento(pedido, StatusPagamento.APROVADO);

        listener.aoProcessarPagamento(EVENTO);
        listener.aoProcessarPagamento(EVENTO);

        verify(notificacaoService, times(4)).notificarPagamento(pedido, StatusPagamento.APROVADO);
        assertThat(mensagens()).last().asString().endsWith("tentativas=1 resultado=ENVIADO");
    }

    @Test
    void deveRegistrarPedidoNaoEncontradoSemRepetir() {
        listener.aoProcessarPagamento(new PagamentoProcessado(4L, 99L, StatusPagamento.APROVADO));

        verify(notificacaoService, times(0)).notificarPagamento(any(), any());
        assertThat(mensagens()).containsExactly(
                "ERROR operacao=notificacao-pagamento pagamentoId=4 pedidoId=99 "
                        + "falha=PEDIDO_NAO_ENCONTRADO resultado=FALHA_DEFINITIVA");
    }

    @Test
    void naoDeveRegistrarDadosSensiveisPresentesNaFalha() {
        doThrow(new IllegalStateException("Destinatário ana@foodnow.test recusado; token=TOKEN_SECRETO"))
                .when(notificacaoService).notificarPagamento(any(), any());

        listener.aoProcessarPagamento(EVENTO);

        assertThat(registros.list).isNotEmpty().allSatisfy(registro -> {
            assertThat(registro.getFormattedMessage()).doesNotContain("ana@foodnow.test", "TOKEN_SECRETO");
            assertThat(registro.getThrowableProxy()).isNull();
        });
    }

    @Test
    void deveIgnorarControleDeDuplicidadeQuandoEventoNaoTemPagamento() {
        PagamentoProcessado semPagamento = new PagamentoProcessado(null, 7L, StatusPagamento.APROVADO);
        doNothing().when(notificacaoService).notificarPagamento(any(), any());

        listener.aoProcessarPagamento(semPagamento);
        listener.aoProcessarPagamento(semPagamento);

        verify(notificacaoService, times(2)).notificarPagamento(pedido, StatusPagamento.APROVADO);
    }

    @Test
    void deveExigirAoMenosUmaTentativa() {
        assertThatThrownBy(() -> new NotificacaoPagamentoListener(pedidoRepository, notificacaoService, 0,
                Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    private List<String> mensagens() {
        return registros.list.stream().map(r -> r.getLevel() + " " + r.getFormattedMessage()).toList();
    }
}
