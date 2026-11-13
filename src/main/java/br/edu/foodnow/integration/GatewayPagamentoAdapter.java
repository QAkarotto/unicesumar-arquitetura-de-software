package br.edu.foodnow.integration;

import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.ProcessadorPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.springframework.stereotype.Component;

/** Traduz o contrato da aplicação para o formato específico do gateway externo. */
@Component
public class GatewayPagamentoAdapter implements ProcessadorPagamento {
    private final FakePaymentGateway gateway;

    public GatewayPagamentoAdapter(FakePaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public ResultadoPagamento processar(SolicitacaoPagamento solicitacao) {
        var requisicao = FakePaymentGateway.GatewayRequest.from(
                solicitacao.pedidoId(), solicitacao.valor(), solicitacao.token(),
                solicitacao.forma().name(), solicitacao.regiao(),
                solicitacao.origem(), solicitacao.destino());
        var resposta = gateway.processarPagamento(requisicao);
        StatusPagamento status = "AUTHORIZED".equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;
        return new ResultadoPagamento(status, resposta.transactionCode(),
                resposta.providerMessage());
    }
}
