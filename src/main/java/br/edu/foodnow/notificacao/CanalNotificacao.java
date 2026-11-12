package br.edu.foodnow.notificacao;

import br.edu.foodnow.model.Notificacao;

/**
 * Contrato da aplicação para o envio de notificações.
 * A regra de negócio conhece este contrato, não o cliente de e-mail utilizado.
 */
@FunctionalInterface
public interface CanalNotificacao {
    void enviar(Notificacao notificacao);
}