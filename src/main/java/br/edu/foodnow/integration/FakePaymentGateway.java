
package br.edu.foodnow.integration;

import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakePaymentGateway implements GatewayPagamento {

    private final Map<String, GatewayResult> transacoes = new ConcurrentHashMap<>();

    @Override
    public ResultadoPagamento processar(SolicitacaoPagamento solicitacao) {
        GatewayRequest request = new GatewayRequest(
                solicitacao.orderReference(),
                solicitacao.amountInCents(),
                solicitacao.paymentToken(),
                solicitacao.paymentMethod(),
                solicitacao.deliveryZone(),
                solicitacao.originCoordinates(),
                solicitacao.destinationCoordinates()
        );

        GatewayResult resultado = processarPagamento(request);

        return new ResultadoPagamento(
                resultado.transactionCode(),
                resultado.providerStatus(),
                resultado.providerMessage()
        );
    }

    public GatewayResult processarPagamento(GatewayRequest request) {
        boolean rejeitado = "REJEITADO".equalsIgnoreCase(request.paymentToken());
        String codigo = "FN-" + request.orderReference();

        GatewayResult resultado = rejeitado
                ? new GatewayResult(codigo, "DECLINED",
                        "Pagamento rejeitado pelo provedor simulado")
                : new GatewayResult(codigo, "AUTHORIZED", "Pagamento autorizado");

        transacoes.put(codigo, resultado);
        return resultado;
    }

    public GatewayResult consultarPagamento(String codigo) {
        return transacoes.get(codigo);
    }

    public record GatewayRequest(
            String orderReference,
            long amountInCents,
            String paymentToken,
            String paymentMethod,
            String deliveryZone,
            String originCoordinates,
            String destinationCoordinates
    ) {
        public static GatewayRequest from(
                Long pedidoId, BigDecimal valor, String token, String forma) {
            return from(pedidoId, valor, token, forma,
                    "UNKNOWN", "UNKNOWN", "UNKNOWN");
        }

        public static GatewayRequest from(
                Long pedidoId, BigDecimal valor, String token, String forma,
                String zona, String origem, String destino) {
            return new GatewayRequest(
                    pedidoId.toString(),
                    valor.movePointRight(2).longValueExact(),
                    token,
                    forma,
                    zona,
                    origem,
                    destino
            );
        }
    }

    public record GatewayResult(
            String transactionCode,
            String providerStatus,
            String providerMessage
    ) {
    }
}
