package br.edu.foodnow.event;

import br.edu.foodnow.integration.FakeEmailClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConfirmacaoPedidoListenerTest {
    @Test
    void deveEnviarEmailComOsDadosDoEvento() {
        FakeEmailClient emailClient = mock(FakeEmailClient.class);
        new ConfirmacaoPedidoListener(emailClient).aoConfirmar(
                new PedidoConfirmado(42L, "cliente@foodnow.test", "CENTRAL", 35));

        verify(emailClient).enviar("cliente@foodnow.test", "Pedido confirmado",
                "O pedido 42 foi confirmado para a região CENTRAL. Estimativa interna: 35 minutos.");
    }

    @Test
    void falhaNoEmailNaoEscapaDoConsumidor() {
        FakeEmailClient emailClient = mock(FakeEmailClient.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("falha simulada"))
                .when(emailClient).enviar(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        assertThatCode(() -> new ConfirmacaoPedidoListener(emailClient).aoConfirmar(
                new PedidoConfirmado(42L, "cliente@foodnow.test", "CENTRAL", 35)))
                .doesNotThrowAnyException();
        verify(emailClient).enviar("cliente@foodnow.test", "Pedido confirmado",
                "O pedido 42 foi confirmado para a região CENTRAL. Estimativa interna: 35 minutos.");
    }
}
