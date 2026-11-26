package br.edu.foodnow.integration;

/** Falha transitória na comunicação com o provedor de mapas, passível de nova tentativa. */
public class MapsIndisponivelException extends RuntimeException {
    public MapsIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
