package br.edu.foodnow.event;

/** Dados da confirmação, sem referências a entidades ou ao contexto de persistência. */
public record PedidoConfirmado(Long pedidoId, String emailCliente, String regiaoEntrega,
                               int tempoEstimadoMinutos) {
}
