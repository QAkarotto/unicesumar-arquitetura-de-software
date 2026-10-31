
package br.edu.foodnow.service;

import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PagamentoServiceDependencyTest {

    private PedidoRepository pedidoRepository;
    private PagamentoGateway pagamentoGateway;
    private PagamentoService pagamentoService;

    @BeforeEach
    void configurar() {

        pedidoRepository = mock(PedidoRepository.class);
        pagamentoGateway = mock(PagamentoGateway.class);

        pagamentoService = new PagamentoService(
                pedidoRepository,
                mock(PagamentoRepository.class),
                pagamentoGateway,
                mock(NotificacaoService.class),
                mock(EntregaService.class),
                mock(LocalizacaoService.class),
                mock(LogisticaService.class)
        );
    }

    @Test
    void deveRejeitarPagamentoDePedidoNaoConfirmado() {

        Pedido pedido = mock(Pedido.class);

        when(pedidoRepository.findById(1L))
                .thenReturn(Optional.of(pedido));

        when(pedido.getStatus())
                .thenReturn(StatusPedido.CRIADO);

        assertThrows(
                RegraNegocioException.class,
                () -> pagamentoService.processar(1L, null, "TOKEN")
        );

        verifyNoInteractions(pagamentoGateway);
    }

    @Test
    void deveRejeitarPagamentoDePedidoInexistente() {

        when(pedidoRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> pagamentoService.processar(99L, null, "TOKEN")
        );

        verifyNoInteractions(pagamentoGateway);
    }
}
