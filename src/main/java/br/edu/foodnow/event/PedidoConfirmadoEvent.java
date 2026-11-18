package br.edu.foodnow.event;

/**
 * Representa o fato de que um pedido foi confirmado.
 *
 * O evento carrega somente o identificador para que o consumidor
 * recupere os dados já persistidos após o commit da transação.
 */
public record PedidoConfirmadoEvent(Long pedidoId) {
}