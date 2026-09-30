package br.edu.foodnow.geo;

import br.edu.foodnow.model.Localizacao;

/**
 * Coordenadas obtidas para um endereço.
 *
 * @param localizacao coordenadas geográficas
 * @param aproximada  {@code true} quando o provedor precisou estimar as coordenadas
 */
public record CoordenadasResolvidas(Localizacao localizacao, boolean aproximada) {
}
