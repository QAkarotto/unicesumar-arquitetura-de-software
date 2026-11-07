package br.edu.foodnow.evento;

import java.math.BigDecimal;
import java.time.Instant;

public record PedidoConfirmado(Long pedidoId, Long clienteId, Long restauranteId,
                               BigDecimal valorTotal, Instant confirmadoEm) {
}
