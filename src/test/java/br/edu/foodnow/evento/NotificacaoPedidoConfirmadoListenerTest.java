package br.edu.foodnow.evento;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.service.NotificacaoService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoPedidoConfirmadoListenerTest {
    private final PedidoRepository repository = mock(PedidoRepository.class);
    private final NotificacaoService notificacaoService = mock(NotificacaoService.class);
    private final NotificacaoPedidoConfirmadoListener listener =
            new NotificacaoPedidoConfirmadoListener(repository, notificacaoService);
    private final PedidoConfirmado evento =
            new PedidoConfirmado(7L, 1L, 2L, new BigDecimal("71.29"), Instant.now());

    @Test
    void deveNotificarOPedidoDoEvento() {
        Pedido pedido = mock(Pedido.class);
        when(repository.findById(7L)).thenReturn(Optional.of(pedido));

        listener.aoConfirmarPedido(evento);

        verify(notificacaoService).notificarConfirmacao(pedido);
    }

    @Test
    void naoDevePropagarFalhaDaNotificacao() {
        when(repository.findById(7L)).thenReturn(Optional.of(mock(Pedido.class)));
        doThrow(new IllegalStateException("smtp indisponível")).when(notificacaoService).notificarConfirmacao(any());

        assertThatCode(() -> listener.aoConfirmarPedido(evento)).doesNotThrowAnyException();
    }

    @Test
    void deveIgnorarPedidoInexistente() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        listener.aoConfirmarPedido(evento);

        verify(notificacaoService, never()).notificarConfirmacao(any());
    }
}
