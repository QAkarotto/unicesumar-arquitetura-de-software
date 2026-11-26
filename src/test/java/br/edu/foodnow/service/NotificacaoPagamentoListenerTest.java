package br.edu.foodnow.service;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.PagamentoProcessado;
import br.edu.foodnow.repository.PedidoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoPagamentoListenerTest {
    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final NotificacaoService notificacaoService = mock(NotificacaoService.class);
    private final NotificacaoPagamentoListener listener =
            new NotificacaoPagamentoListener(pedidoRepository, notificacaoService);

    @Test
    void deveNotificarClienteComOStatusDoEvento() {
        Pedido pedido = mock(Pedido.class);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));

        listener.aoProcessarPagamento(new PagamentoProcessado(3L, 7L, StatusPagamento.REJEITADO));

        verify(notificacaoService).notificarPagamento(pedido, StatusPagamento.REJEITADO);
    }

    @Test
    void naoDevePropagarFalhaDoEnvioDeEmail() {
        Pedido pedido = mock(Pedido.class);
        when(pedidoRepository.findById(7L)).thenReturn(Optional.of(pedido));
        doThrow(new IllegalStateException("SMTP indisponível"))
                .when(notificacaoService).notificarPagamento(any(), any());

        assertThatCode(() -> listener.aoProcessarPagamento(
                new PagamentoProcessado(3L, 7L, StatusPagamento.APROVADO))).doesNotThrowAnyException();
    }

    @Test
    void naoDevePropagarFalhaQuandoPedidoNaoEncontrado() {
        when(pedidoRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatCode(() -> listener.aoProcessarPagamento(
                new PagamentoProcessado(4L, 8L, StatusPagamento.APROVADO))).doesNotThrowAnyException();
        verify(notificacaoService, never()).notificarPagamento(any(), any());
    }
}
