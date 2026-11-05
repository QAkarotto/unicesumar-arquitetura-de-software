package br.edu.foodnow.service;

import br.edu.foodnow.model.Notificacao;

public interface EnvioNotificacao {
    void enviar(String destinatario, String assunto, String mensagem);

    void enviar(Notificacao notificacao);
}
