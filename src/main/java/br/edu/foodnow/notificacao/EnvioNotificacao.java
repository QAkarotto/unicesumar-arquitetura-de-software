package br.edu.foodnow.notificacao;

import br.edu.foodnow.model.Notificacao;

/** Contrato de envio usado pela aplicação sem depender de um cliente concreto. */
public interface EnvioNotificacao {
    void enviar(String destinatario, String assunto, String mensagem);

    void enviar(Notificacao notificacao);
}
