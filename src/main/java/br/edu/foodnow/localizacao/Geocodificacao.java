package br.edu.foodnow.localizacao;

import br.edu.foodnow.model.Localizacao;

/**
 * Coordenadas resolvidas para um endereço, com a indicação de que o provedor apenas aproximou o
 * resultado em vez de reconhecer o endereço exato.
 */
public record Geocodificacao(Localizacao localizacao, boolean aproximada) {
}
