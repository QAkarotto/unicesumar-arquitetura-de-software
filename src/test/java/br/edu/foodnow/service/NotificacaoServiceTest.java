
package br.edu.foodnow.service;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.notificacao.EmailGateway;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificacaoServiceTest {

    @Test
    void deveEnviarNotificacaoDeInicioDeEntregaPeloContrato() {
        // Arrange: implementação de teste da abstração.
        EmailGatewayEmMemoria emailGateway = new EmailGatewayEmMemoria();

        Cliente cliente = mock(Cliente.class);
        Pedido pedido = mock(Pedido.class);

        when(cliente.getEmail()).thenReturn("cliente@foodnow.com");
        when(pedido.getCliente()).thenReturn(cliente);
        when(pedido.getId()).thenReturn(42L);

        NotificacaoService service = new NotificacaoService(
                emailGateway,
                mock(LocalizacaoService.class),
                mock(br.edu.foodnow.logistica.LogisticaService.class)
        );

        // Act: executa a regra da aplicação.
        service.notificarInicioEntrega(pedido);

        // Assert: verifica o resultado pelo contrato.
        assertEquals(1, emailGateway.notificacoes.size());

        Notificacao enviada = emailGateway.notificacoes.get(0);

        assertEquals("cliente@foodnow.com", enviada.getDestinatario());
        assertEquals("Entrega iniciada", enviada.getAssunto());
        assertEquals(
                "O pedido 42 saiu para entrega.",
                enviada.getMensagem()
        );
    }

    /**
     * Test double que implementa o contrato sem depender
     * da implementação FakeEmailClient.
     */
    private static class EmailGatewayEmMemoria implements EmailGateway {

        private final List<Notificacao> notificacoes = new ArrayList<>();

        @Override
        public void enviar(String destinatario, String assunto, String mensagem) {
            notificacoes.add(new Notificacao(destinatario, assunto, mensagem));
        }

        @Override
        public void enviar(Notificacao notificacao) {
            notificacoes.add(notificacao);
        }
    }
}
