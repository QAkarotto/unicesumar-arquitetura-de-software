package br.edu.foodnow.pagamento;

public interface GatewayPagamento {

    ResultadoPagamento processar(SolicitacaoPagamento solicitacao);
}
