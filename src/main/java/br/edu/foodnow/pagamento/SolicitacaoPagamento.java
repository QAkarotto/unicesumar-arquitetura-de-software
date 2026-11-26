package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;

import java.math.BigDecimal;

public record SolicitacaoPagamento(Long pedidoId, BigDecimal valor, String token, FormaPagamento forma,
                                   String regiaoEntrega, Localizacao origem, Localizacao destino) {

    /** Chave estável por pedido: repetir a solicitação não deve gerar uma nova cobrança. */
    public String chaveIdempotencia() {
        return "pedido-" + pedidoId;
    }
}
