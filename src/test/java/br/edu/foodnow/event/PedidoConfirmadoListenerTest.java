package br.edu.foodnow.event;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.service.NotificacaoService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

class PedidoConfirmadoListenerTest {

    @Test
    void deveNotificarQuandoReceberEventoDeConfirmacao() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        NotificacaoService notificacoes = mock(NotificacaoService.class);
        Pedido pedido = mock(Pedido.class);
        when(pedidos.findById(15L)).thenReturn(Optional.of(pedido));

        new PedidoConfirmadoListener(pedidos, notificacoes)
                .aoConfirmarPedido(new PedidoConfirmadoEvent(15L));

        verify(notificacoes).notificarConfirmacao(pedido);
    }

    @Test
    void falhaNaNotificacaoNaoInterrompeOConsumidor() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        NotificacaoService notificacoes = mock(NotificacaoService.class);
        Pedido pedido = mock(Pedido.class);
        when(pedidos.findById(15L)).thenReturn(Optional.of(pedido));
        doThrow(new IllegalStateException("Serviço de email indisponível"))
                .when(notificacoes).notificarConfirmacao(pedido);

        new PedidoConfirmadoListener(pedidos, notificacoes)
                .aoConfirmarPedido(new PedidoConfirmadoEvent(15L));

        verify(notificacoes).notificarConfirmacao(pedido);
    }
}
