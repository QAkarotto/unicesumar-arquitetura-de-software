package br.edu.foodnow.integration;

/** Falha de comunicação no envio; não garante se o provedor aceitou a mensagem. */
public class EmailIndisponivelException extends RuntimeException {
    public EmailIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
