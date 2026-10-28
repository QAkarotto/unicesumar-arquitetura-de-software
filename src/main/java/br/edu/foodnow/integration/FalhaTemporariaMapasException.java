package br.edu.foodnow.integration;

/** Indica uma falha transitória do provedor de mapas, como timeout ou indisponibilidade temporária. */
public class FalhaTemporariaMapasException extends RuntimeException {
    public FalhaTemporariaMapasException(String mensagem) {
        super(mensagem);
    }

}
