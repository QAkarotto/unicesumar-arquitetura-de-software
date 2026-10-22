package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;

import java.math.BigDecimal;

public record SolicitacaoPagamento(Long pedidoId, BigDecimal valor, FormaPagamento forma, String token,
                                   String regiaoEntrega, Localizacao origem, Localizacao destino) {
}
