package br.edu.foodnow.integration;

/** Indica uma falha temporária do provedor de mapas, como timeout ou indisponibilidade. */
public class FalhaTemporariaMapsException extends RuntimeException {

    public FalhaTemporariaMapsException(String mensagem) {
        super(mensagem);
    }

    public FalhaTemporariaMapsException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
