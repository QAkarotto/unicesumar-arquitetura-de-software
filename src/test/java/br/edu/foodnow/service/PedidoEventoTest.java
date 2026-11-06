
package br.edu.foodnow.service;

import br.edu.foodnow.event.PedidoConfirmadoEvent;
import br.edu.foodnow.event.PedidoConfirmadoListener;
import br.edu.foodnow.event.RegistroConfirmacaoLog;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.repository.ClienteRepository;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.repository.ProdutoRepository;
import br.edu.foodnow.repository.RestauranteRepository;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidoEventoTest {

    @Test
    void devePublicarEventoAoConfirmarPedido() {

        PedidoRepository repositorio = mock(PedidoRepository.class);
        Pedido pedido = mock(Pedido.class);
        ApplicationEventPublisher publicador =
                mock(ApplicationEventPublisher.class);

        when(repositorio.findById(1L))
                .thenReturn(Optional.of(pedido));

        when(repositorio.save(pedido)).thenReturn(pedido);
        when(pedido.getId()).thenReturn(1L);

        NotificacaoService notificacao =
                mock(NotificacaoService.class);

        PedidoService service = criarService(
                repositorio, notificacao, publicador
        );

        Pedido resultado = service.confirmar(1L);

        assertSame(pedido, resultado);

        verify(pedido).confirmar();
        verify(repositorio).save(pedido);
        verify(notificacao).notificarConfirmacao(pedido);
        verify(publicador).publishEvent(
                new PedidoConfirmadoEvent(1L)
        );
    }

    @Test
    void falhaNoConsumidorNaoImpedeConfirmacao() {

        PedidoRepository repositorio = mock(PedidoRepository.class);
        Pedido pedido = mock(Pedido.class);

        when(repositorio.findById(2L))
                .thenReturn(Optional.of(pedido));

        when(repositorio.save(pedido)).thenReturn(pedido);
        when(pedido.getId()).thenReturn(2L);

        RegistroConfirmacaoLog registro =
                mock(RegistroConfirmacaoLog.class);

        doThrow(new RuntimeException("Falha no log"))
                .when(registro).registrar(2L);

        PedidoConfirmadoListener listener =
                new PedidoConfirmadoListener(registro);

        ApplicationEventPublisher publicador =
                mock(ApplicationEventPublisher.class);

        doAnswer(chamada -> {
            PedidoConfirmadoEvent evento =
                    chamada.getArgument(
                            0, PedidoConfirmadoEvent.class
                    );

            listener.processar(evento);
            return null;

        }).when(publicador).publishEvent(any(Object.class));

        PedidoService service = criarService(
                repositorio,
                mock(NotificacaoService.class),
                publicador
        );

        Pedido resultado = assertDoesNotThrow(
                () -> service.confirmar(2L)
        );

        assertSame(pedido, resultado);

        verify(pedido).confirmar();
        verify(repositorio).save(pedido);
        verify(registro).registrar(2L);
    }

    private PedidoService criarService(
            PedidoRepository repositorio,
            NotificacaoService notificacao,
            ApplicationEventPublisher publicador) {

        PedidoService service = new PedidoService(
                mock(ClienteRepository.class),
                mock(RestauranteRepository.class),
                mock(ProdutoRepository.class),
                repositorio,
                mock(LogisticaService.class),
                mock(PagamentoService.class),
                notificacao
        );

        service.configurarPublicador(publicador);

        return service;
    }
}
