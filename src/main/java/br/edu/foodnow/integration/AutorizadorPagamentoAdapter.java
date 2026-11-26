package br.edu.foodnow.integration;

import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.AutorizadorPagamento;
import org.springframework.stereotype.Component;

@Component
public class AutorizadorPagamentoAdapter implements AutorizadorPagamento {
    private final FakePaymentGateway gateway;

    public AutorizadorPagamentoAdapter(FakePaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public Resultado autorizar(Solicitacao solicitacao) {
        var request = FakePaymentGateway.GatewayRequest.from(solicitacao.pedidoId(), solicitacao.valor(),
                solicitacao.token(), solicitacao.forma().name(), solicitacao.regiao(),
                solicitacao.origem().formatarParaProvedor(), solicitacao.destino().formatarParaProvedor());
        var result = gateway.processarPagamento(request);
        StatusPagamento status = "AUTHORIZED".equals(result.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;
        return new Resultado(status, result.transactionCode(), result.providerMessage());
    }
}
