package br.edu.foodnow.evento;

/** Dados do início da entrega, sem transportar entidades JPA para outra thread. */
public record EntregaIniciada(Long entregaId, Long pedidoId, String emailCliente) {
}
