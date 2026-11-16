package br.edu.foodnow.event;

/** Fato de negócio com dados suficientes para notificar sem consultar entidades JPA. */
public record PedidoConfirmado(Long pedidoId, String emailCliente, String regiao, int estimativaMinutos) {
}
