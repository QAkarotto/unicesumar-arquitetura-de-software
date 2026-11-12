package br.edu.foodnow.integration;

import br.edu.foodnow.model.Notificacao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailNotificacaoAdapterTest {

    @Test
    void deveEncaminharNotificacaoAoClienteDeEmail() {
        FakeEmailClient emailClient = new FakeEmailClient();

        EmailNotificacaoAdapter adapter =
                new EmailNotificacaoAdapter(emailClient);

        Notificacao notificacao = new Notificacao(
                "cliente@foodnow.test",
                "Pedido confirmado",
                "O pedido 42 foi confirmado."
        );

        adapter.enviar(notificacao);

        assertThat(emailClient.getEnviadas()).hasSize(1);
        assertThat(emailClient.getEnviadas().getFirst())
                .isSameAs(notificacao);
    }
}