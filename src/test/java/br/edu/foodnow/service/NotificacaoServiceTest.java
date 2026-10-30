package br.edu.foodnow.service;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.logistica.LogisticaService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificacaoServiceTest {

    @Test
    void deveNotificarInicioDaEntregaSemUsarClienteDeEmailConcreto() {
        EnvioNotificacao envio = mock(EnvioNotificacao.class);
        Pedido pedido = mock(Pedido.class);
        when(pedido.getCliente()).thenReturn(new Cliente("Ana", "ana@foodnow.test"));
        when(pedido.getId()).thenReturn(10L);

        NotificacaoService service = new NotificacaoService(envio,
                mock(LocalizacaoService.class), mock(LogisticaService.class));
        service.notificarInicioEntrega(pedido);

        verify(envio).enviar("ana@foodnow.test", "Entrega iniciada",
                "O pedido 10 saiu para entrega.");
        verifyNoMoreInteractions(envio);
    }
}
