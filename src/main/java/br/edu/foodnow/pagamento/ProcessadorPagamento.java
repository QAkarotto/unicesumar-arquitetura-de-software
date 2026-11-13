package br.edu.foodnow.pagamento;

/** Fronteira da aplicação para processamento de pagamentos. */
public interface ProcessadorPagamento {
    ResultadoPagamento processar(SolicitacaoPagamento solicitacao);
}
