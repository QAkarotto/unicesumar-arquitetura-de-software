package br.edu.foodnow.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FakePaymentGateway {

    private static final Logger log =
            LoggerFactory.getLogger(FakePaymentGateway.class);

    private final Map<String, GatewayResult> transacoes =
            new ConcurrentHashMap<>();

    private final Map<String, GatewayResult> resultadosPorPedido =
            new ConcurrentHashMap<>();

    public GatewayResult processarPagamento(GatewayRequest request) {

        AtomicBoolean processadoNestaChamada = new AtomicBoolean(false);

        // O identificador do pedido funciona como chave de idempotência.
        GatewayResult resultado = resultadosPorPedido.computeIfAbsent(
                request.orderReference(), pedidoId -> {

                    processadoNestaChamada.set(true);

                    boolean rejeitado =
                            "REJEITADO".equalsIgnoreCase(request.paymentToken());

                    String codigo = "FN-" + pedidoId;

                    GatewayResult novoResultado = rejeitado
                            ? new GatewayResult(
                                    codigo,
                                    "DECLINED",
                                    "Pagamento rejeitado pelo provedor simulado"
                            )
                            : new GatewayResult(
                                    codigo,
                                    "AUTHORIZED",
                                    "Pagamento autorizado"
                            );

                    transacoes.put(codigo, novoResultado);

                    return novoResultado;
                }
        );

        if (processadoNestaChamada.get()) {

            log.info(
                    "operacao=PROCESSAR_PAGAMENTO pedidoId={} resultado={} codigoTransacao={}",
                    request.orderReference(),
                    resultado.providerStatus(),
                    resultado.transactionCode()
            );

        } else {

            log.warn(
                    "operacao=PROCESSAR_PAGAMENTO pedidoId={} tipoFalha=REQUISICAO_DUPLICADA " +
                            "tratamento=RESULTADO_ANTERIOR_REUTILIZADO codigoTransacao={} resultado={}",
                    request.orderReference(),
                    resultado.transactionCode(),
                    resultado.providerStatus()
            );
        }

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
                Long pedidoId,
                BigDecimal valor,
                String token,
                String forma
        ) {
            return from(
                    pedidoId,
                    valor,
                    token,
                    forma,
                    "UNKNOWN",
                    "UNKNOWN",
                    "UNKNOWN"
            );
        }

        public static GatewayRequest from(
                Long pedidoId,
                BigDecimal valor,
                String token,
                String forma,
                String zona,
                String origem,
                String destino
        ) {
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