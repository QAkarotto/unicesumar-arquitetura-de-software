package br.edu.foodnow.integration;

import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.GatewayPagamentoIndisponivelException;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;

/**
 * Traduz o contrato {@link GatewayPagamento} para o formato do provedor simulado.
 * Falhas técnicas do provedor são convertidas em {@link GatewayPagamentoIndisponivelException}.
 */
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
                solicitacao.destino().formatarParaProvedor(), solicitacao.chaveIdempotencia());
        FakePaymentGateway.GatewayResult resposta;
        try {
            resposta = paymentGateway.processarPagamento(requisicao);
        } catch (RuntimeException falha) {
            throw new GatewayPagamentoIndisponivelException("Falha na comunicação com o provedor de pagamento",
                    falha);
        }
        StatusPagamento status = STATUS_AUTORIZADO.equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;
        return new ResultadoPagamento(status, resposta.transactionCode(), resposta.providerMessage());
    }
}
