package br.edu.foodnow.integration;

/** Falha operacional de disponibilidade do provedor de e-mail. */
public class EmailIndisponivelException extends RuntimeException {
    public EmailIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
