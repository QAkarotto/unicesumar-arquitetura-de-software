package br.edu.foodnow.pagamento;

/** Limite entre o caso de uso de pagamento e o provedor que de fato autoriza a cobrança. */
public interface ProcessadorPagamento {
    ResultadoPagamento processar(SolicitacaoPagamento solicitacao);
}
