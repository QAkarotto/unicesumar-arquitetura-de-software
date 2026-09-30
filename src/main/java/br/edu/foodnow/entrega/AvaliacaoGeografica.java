package br.edu.foodnow.entrega;

import br.edu.foodnow.geo.Rota;

/**
 * Avaliação geográfica de um pedido já criado, usada no pagamento e nas notificações.
 *
 * @param rota   rota restaurante -> endereço de entrega
 * @param regiao região de entrega (CENTRAL, URBANA, EXTERNA ou NAO_ATENDIDA)
 */
public record AvaliacaoGeografica(Rota rota, String regiao) {
}
