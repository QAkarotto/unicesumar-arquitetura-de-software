package br.edu.foodnow.integration;

import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.ProcessadorPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.springframework.stereotype.Component;

/** Traduz o contrato de pagamento da aplicação para o formato do gateway simulado. */
@Component
public class GatewayPagamentoAdapter implements ProcessadorPagamento {
    private final FakePaymentGateway gateway;

    public GatewayPagamentoAdapter(FakePaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public ResultadoPagamento processar(SolicitacaoPagamento solicitacao) {
        var requisicao = FakePaymentGateway.GatewayRequest.from(solicitacao.pedidoId(), solicitacao.valor(),
                solicitacao.token(), solicitacao.forma().name(), solicitacao.regiao(),
                solicitacao.origem().formatarParaProvedor(), solicitacao.destino().formatarParaProvedor());
        var resposta = gateway.processarPagamento(requisicao);
        StatusPagamento status = "AUTHORIZED".equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;
        return new ResultadoPagamento(status, resposta.transactionCode(), resposta.providerMessage());
    }
}
