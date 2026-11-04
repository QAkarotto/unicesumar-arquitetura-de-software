package br.edu.foodnow.service;

public class FalhaIntegracaoException extends RuntimeException {

    public FalhaIntegracaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public FalhaIntegracaoException(String mensagem) {
        super(mensagem);
    }
}