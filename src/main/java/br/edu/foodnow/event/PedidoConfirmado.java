package br.edu.foodnow.event;

/** Dados imutáveis da confirmação, sem entidades ou acesso posterior ao banco. */
public record PedidoConfirmado(Long pedidoId, String destinatario, String regiao,
                               int tempoEstimadoMinutos) {
}
