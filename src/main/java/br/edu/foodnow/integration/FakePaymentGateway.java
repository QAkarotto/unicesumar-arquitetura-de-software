package br.edu.foodnow.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class FakePaymentGateway {
    private final Map<String, GatewayResult> transacoes = new ConcurrentHashMap<>();
    private final Map<String, GatewayResult> porChaveIdempotencia = new ConcurrentHashMap<>();
    private final AtomicInteger cobrancasProcessadas = new AtomicInteger();

    /**
     * Processa o pagamento. Requisições repetidas com a mesma chave de idempotência devolvem a
     * transação já processada, sem nova cobrança.
     */
    public GatewayResult processarPagamento(GatewayRequest request) {
        return porChaveIdempotencia.computeIfAbsent(request.idempotencyKey(), chave -> processar(request));
    }

    private GatewayResult processar(GatewayRequest request) {
        cobrancasProcessadas.incrementAndGet();
        boolean rejeitado = "REJEITADO".equalsIgnoreCase(request.paymentToken());
        String codigo = "FN-" + request.orderReference();
        GatewayResult resultado = rejeitado
                ? new GatewayResult(codigo, "DECLINED", "Pagamento rejeitado pelo provedor simulado")
                : new GatewayResult(codigo, "AUTHORIZED", "Pagamento autorizado");
        transacoes.put(codigo, resultado);
        return resultado;
    }

    public int getCobrancasProcessadas() {
        return cobrancasProcessadas.get();
    }

    public GatewayResult consultarPagamento(String codigo) {
        return transacoes.get(codigo);
    }

    public record GatewayRequest(String orderReference, long amountInCents, String paymentToken,
                                 String paymentMethod, String deliveryZone,
                                 String originCoordinates, String destinationCoordinates,
                                 String idempotencyKey) {
        public static GatewayRequest from(Long pedidoId, BigDecimal valor, String token, String forma) {
            return from(pedidoId, valor, token, forma, "UNKNOWN", "UNKNOWN", "UNKNOWN");
        }

        public static GatewayRequest from(Long pedidoId, BigDecimal valor, String token, String forma,
                                          String zona, String origem, String destino) {
            return from(pedidoId, valor, token, forma, zona, origem, destino, "FN-" + pedidoId);
        }

        public static GatewayRequest from(Long pedidoId, BigDecimal valor, String token, String forma,
                                          String zona, String origem, String destino, String chave) {
            return new GatewayRequest(pedidoId.toString(), valor.movePointRight(2).longValueExact(), token,
                    forma, zona, origem, destino, chave);
        }
    }

    public record GatewayResult(String transactionCode, String providerStatus, String providerMessage) {
    }
}
