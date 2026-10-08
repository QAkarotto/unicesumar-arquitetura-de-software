package br.edu.foodnow.localizacao;

/**
 * Condições de entrega já resolvidas para um pedido, a rota reconciliada e a decisão de
 * atendimento tomadas em conjunto.
 *
 * <p>Existe para que o caso de uso de pedido receba um resultado pronto em vez de precisar
 * orquestrar distância, área e preço por conta própria.</p>
 */
public record PlanoDeEntrega(RotaEntrega rota, boolean atendido) {

    public double distanciaKm() {
        return rota.distanciaKm();
    }
}
