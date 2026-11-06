package br.edu.foodnow.service;

import br.edu.foodnow.event.PedidoConfirmadoEvent;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.repository.ClienteRepository;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.repository.ProdutoRepository;
import br.edu.foodnow.repository.RestauranteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PedidoConfirmacaoEventoTest {

    @Test
    void devePublicarEventoAposSalvarPedidoConfirmado() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
        Pedido pedido = mock(Pedido.class);
        when(pedidos.findById(15L)).thenReturn(Optional.of(pedido));
        when(pedidos.save(pedido)).thenReturn(pedido);
        when(pedido.getId()).thenReturn(15L);
        PedidoService service = new PedidoService(mock(ClienteRepository.class),
                mock(RestauranteRepository.class), mock(ProdutoRepository.class), pedidos,
                mock(LogisticaService.class), mock(PagamentoService.class), eventos);

        assertThat(service.confirmar(15L)).isSameAs(pedido);

        var ordem = inOrder(pedido, pedidos, eventos);
        ordem.verify(pedido).confirmar();
        ordem.verify(pedidos).save(pedido);
        ordem.verify(eventos).publishEvent(new PedidoConfirmadoEvent(15L));
    }

    @Test
    void naoPublicaEventoQuandoConfirmacaoFalha() {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
        Pedido pedido = mock(Pedido.class);
        when(pedidos.findById(15L)).thenReturn(Optional.of(pedido));
        doThrow(new IllegalStateException("Pedido inválido")).when(pedido).confirmar();
        PedidoService service = new PedidoService(mock(ClienteRepository.class),
                mock(RestauranteRepository.class), mock(ProdutoRepository.class), pedidos,
                mock(LogisticaService.class), mock(PagamentoService.class), eventos);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.confirmar(15L))
                .isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(eventos);
        verify(pedidos, never()).save(any());
    }
}
