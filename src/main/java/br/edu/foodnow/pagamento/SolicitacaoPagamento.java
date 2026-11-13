package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.FormaPagamento;

import java.math.BigDecimal;

/** Dados que o caso de uso fornece para processar um pagamento. */
public record SolicitacaoPagamento(Long pedidoId, BigDecimal valor, String token,
                                   FormaPagamento forma, String regiao,
                                   String origem, String destino) {
}
