package br.edu.foodnow.integration;

import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.notificacao.CanalNotificacao;
import org.springframework.stereotype.Component;

/** Adapta o contrato da aplicação ao cliente de e-mail simulado do FoodNow. */
@Component
public class EmailNotificacaoAdapter implements CanalNotificacao {

    private final FakeEmailClient emailClient;

    public EmailNotificacaoAdapter(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Override
    public void enviar(Notificacao notificacao) {
        emailClient.enviar(notificacao);
    }
}