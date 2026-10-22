package br.edu.foodnow.service;

import java.math.BigDecimal;

/** Contrato da aplicação para solicitar o processamento de um pagamento. */
public interface GatewayPagamento {
    ResultadoPagamento processarPagamento(RequisicaoPagamento requisicao);

    record RequisicaoPagamento(Long pedidoId, BigDecimal valor, String token, String forma,
                               String regiaoEntrega, String origem, String destino) {
    }

    record ResultadoPagamento(boolean aprovado, String codigoTransacao, String mensagem) {
    }
}
