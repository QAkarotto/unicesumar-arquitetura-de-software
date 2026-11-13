package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.StatusPagamento;

/** Resultado estável para a aplicação, sem expor códigos do provedor. */
public record ResultadoPagamento(StatusPagamento status, String codigoExterno,
                                 String mensagemProvedor) {
}
