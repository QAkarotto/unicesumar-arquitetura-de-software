package br.edu.foodnow.service;

import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.notificacao.EnvioNotificacao;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceTest {

    @Test
    void notificarInicioEntregaPodeSerTestadoSemClienteDeEmailConcreto() {
        EnvioNotificacao envio = mock(EnvioNotificacao.class);
        LocalizacaoService localizacao = mock(LocalizacaoService.class);
        LogisticaService logistica = mock(LogisticaService.class);
        NotificacaoService service = new NotificacaoService(envio, localizacao, logistica);

        Cliente cliente = mock(Cliente.class);
        when(cliente.getEmail()).thenReturn("cliente@foodnow.test");
        Pedido pedido = mock(Pedido.class);
        when(pedido.getCliente()).thenReturn(cliente);
        when(pedido.getId()).thenReturn(73L);

        service.notificarInicioEntrega(pedido);

        verify(envio).enviar("cliente@foodnow.test", "Entrega iniciada", "O pedido 73 saiu para entrega.");
    }
}
