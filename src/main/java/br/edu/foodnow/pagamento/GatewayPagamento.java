package br.edu.foodnow.pagamento;

/**
 * Contrato de autorização de pagamentos exigido pela regra da aplicação.
 * Implementações concretas (provedores externos) ficam fora deste pacote.
 */
public interface GatewayPagamento {
    ResultadoPagamento autorizar(SolicitacaoPagamento solicitacao);
}
