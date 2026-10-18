package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.StatusPagamento;

public record ResultadoPagamento(StatusPagamento status, String codigoTransacao, String mensagem) {
}
