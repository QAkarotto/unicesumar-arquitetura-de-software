
package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Pedido;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceRetryTest {

    @Test
    void deveTentarNovamenteQuandoOEnvioFalharTemporariamente() {
        FakeEmailClient emailClient = mock(FakeEmailClient.class);

        NotificacaoService service = new NotificacaoService(
                emailClient,
                mock(LocalizacaoService.class),
                mock(LogisticaService.class)
        );

        Pedido pedido = criarPedido();

        doThrow(new IllegalStateException("Falha temporária"))
                .doNothing()
                .when(emailClient)
                .enviar(anyString(), anyString(), anyString());

        service.notificarInicioEntrega(pedido);

        verify(emailClient, times(2))
                .enviar(anyString(), anyString(), anyString());
    }

    @Test
    void deveEncerrarAsTentativasSemInterromperOFluxoPrincipal() {
        FakeEmailClient emailClient = mock(FakeEmailClient.class);

        NotificacaoService service = new NotificacaoService(
                emailClient,
                mock(LocalizacaoService.class),
                mock(LogisticaService.class)
        );

        Pedido pedido = criarPedido();

        doThrow(new IllegalStateException("Serviço indisponível"))
                .when(emailClient)
                .enviar(anyString(), anyString(), anyString());

        assertThatCode(() -> service.notificarInicioEntrega(pedido))
                .doesNotThrowAnyException();

        verify(emailClient, times(3))
                .enviar(anyString(), anyString(), anyString());
    }

    private Pedido criarPedido() {
        Pedido pedido = mock(Pedido.class);
        Cliente cliente = mock(Cliente.class);

        when(pedido.getCliente()).thenReturn(cliente);
        when(pedido.getId()).thenReturn(123L);
        when(cliente.getEmail()).thenReturn("cliente@foodnow.test");

        return pedido;
    }
}
