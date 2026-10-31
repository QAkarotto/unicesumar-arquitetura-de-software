
package br.edu.foodnow.service;

import java.math.BigDecimal;

public interface PagamentoGateway {

    Resposta processar(Solicitacao solicitacao);

    record Solicitacao(
            Long pedidoId,
            BigDecimal valor,
            String token,
            String forma,
            String regiao,
            String origem,
            String destino
    ) {
    }

    record Resposta(
            String codigoTransacao,
            String statusProvedor,
            String mensagemProvedor
    ) {
    }
}
