package br.edu.foodnow.event;

public record EntregaIniciadaEvent(
        Long entregaId,
        Long pedidoId,
        String destinatario
) {
}