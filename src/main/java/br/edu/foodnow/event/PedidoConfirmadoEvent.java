package br.edu.foodnow.event;

/** Fato publicado quando a confirmação do pedido foi persistida na transação. */
public record PedidoConfirmadoEvent(
        Long pedidoId,
        String emailCliente,
        String regiaoEntrega,
        int tempoEstimadoMinutos) {
}
