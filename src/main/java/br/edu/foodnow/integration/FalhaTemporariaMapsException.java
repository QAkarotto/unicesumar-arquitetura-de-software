package br.edu.foodnow.integration;

/**
 * Representa uma falha transitória na comunicação com o provedor de mapas.
 *
 * O cliente de infraestrutura deve utilizar essa exceção para sinalizar
 * falhas que podem ser recuperadas por uma nova tentativa.
 */
public class FalhaTemporariaMapsException extends RuntimeException {

    public FalhaTemporariaMapsException(String message) {
        super(message);
    }

    public FalhaTemporariaMapsException(String message, Throwable cause) {
        super(message, cause);
    }
}