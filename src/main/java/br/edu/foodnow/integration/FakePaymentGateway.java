package br.edu.foodnow.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakePaymentGateway {
    private final Map<String, GatewayResult> transacoes = new ConcurrentHashMap<>();

    public GatewayResult processarPagamento(GatewayRequest request) {
        boolean rejeitado = "REJEITADO".equalsIgnoreCase(request.paymentToken());
        String codigo = "FN-" + request.orderReference();
        GatewayResult resultado = rejeitado
                ? new GatewayResult(codigo, "DECLINED", "Pagamento rejeitado pelo provedor simulado")
                : new GatewayResult(codigo, "AUTHORIZED", "Pagamento autorizado");
        transacoes.put(codigo, resultado);
        return resultado;
    }

    public GatewayResult consultarPagamento(String codigo) {
        return transacoes.get(codigo);
    }

    public record GatewayRequest(String orderReference, long amountInCents, String paymentToken,
                                 String paymentMethod) {
        public static GatewayRequest from(Long pedidoId, BigDecimal valor, String token, String forma) {
            return new GatewayRequest(pedidoId.toString(), valor.movePointRight(2).longValueExact(), token, forma);
        }
    }

    public record GatewayResult(String transactionCode, String providerStatus, String providerMessage) {
    }
}
