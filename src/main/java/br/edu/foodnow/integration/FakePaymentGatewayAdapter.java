package br.edu.foodnow.integration;

import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.springframework.stereotype.Component;

/**
 * Traduz o contrato {@link GatewayPagamento} para o formato do provedor simulado.
 */
@Component
public class FakePaymentGatewayAdapter implements GatewayPagamento {
    private static final String STATUS_AUTORIZADO = "AUTHORIZED";

    private final FakePaymentGateway paymentGateway;

    public FakePaymentGatewayAdapter(FakePaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    @Override
    public ResultadoPagamento autorizar(SolicitacaoPagamento solicitacao) {
        FakePaymentGateway.GatewayRequest requisicao = FakePaymentGateway.GatewayRequest.from(
                solicitacao.pedidoId(), solicitacao.valor(), solicitacao.token(), solicitacao.forma().name(),
                solicitacao.regiaoEntrega(), solicitacao.origem().formatarParaProvedor(),
                solicitacao.destino().formatarParaProvedor());
        FakePaymentGateway.GatewayResult resposta = paymentGateway.processarPagamento(requisicao);
        StatusPagamento status = STATUS_AUTORIZADO.equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;
        return new ResultadoPagamento(status, resposta.transactionCode(), resposta.providerMessage());
    }
}
