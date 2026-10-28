package br.edu.foodnow.Exceptions;

public class ServicoLocalizacaoIndisponivelException extends RuntimeException {
    public ServicoLocalizacaoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
