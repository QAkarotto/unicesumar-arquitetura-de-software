package br.edu.foodnow.entrega;

import java.math.BigDecimal;

/**
 * Resultado da cotação de entrega feita na criação do pedido.
 *
 * @param distanciaKm distância efetiva considerada (gravada no pedido)
 * @param atendido    se o endereço está dentro da área de atendimento
 * @param taxaBase    taxa de entrega antes do adicional dos itens (zero quando não atendido)
 */
public record CotacaoEntrega(double distanciaKm, boolean atendido, BigDecimal taxaBase) {

    public static CotacaoEntrega foraDaArea(double distanciaKm) {
        return new CotacaoEntrega(distanciaKm, false, BigDecimal.ZERO);
    }
}
