package br.edu.foodnow.integration;

import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.ProcessadorPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.springframework.stereotype.Component;

@Component
public class GatewayPagamentoAdapter implements ProcessadorPagamento {
    private final FakePaymentGateway paymentGateway;

    public GatewayPagamentoAdapter(FakePaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    @Override
    public ResultadoPagamento processar(SolicitacaoPagamento solicitacao) {
        var requisicao = FakePaymentGateway.GatewayRequest.from(solicitacao.pedidoId(), solicitacao.valor(),
                solicitacao.token(), solicitacao.forma().name(), solicitacao.regiaoEntrega(),
                solicitacao.origem().formatarParaProvedor(), solicitacao.destino().formatarParaProvedor());
        var resposta = paymentGateway.processarPagamento(requisicao);
        StatusPagamento status = "AUTHORIZED".equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;
        return new ResultadoPagamento(status, resposta.transactionCode(), resposta.providerMessage());
    }
}
