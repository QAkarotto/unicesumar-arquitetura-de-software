package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.StatusPagamento;

/**
 * Evento de domínio: o pagamento de um pedido foi processado (aprovado ou rejeitado) e registrado.
 */
public record PagamentoProcessado(Long pagamentoId, Long pedidoId, StatusPagamento status) {
}
