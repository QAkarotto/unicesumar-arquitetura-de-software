package br.edu.foodnow.pagamento;

/** Contrato que a aplicação usa para cobrar um pedido, sem conhecer o provedor de pagamento. */
public interface ProcessadorPagamento {
    ResultadoPagamento processar(SolicitacaoPagamento solicitacao);
}
