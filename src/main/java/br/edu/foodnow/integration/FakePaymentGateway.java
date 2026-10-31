
package br.edu.foodnow.integration;

import br.edu.foodnow.service.PagamentoGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FakePaymentGateway implements PagamentoGateway {

    private final Map<String, GatewayResult> transacoes =
            new ConcurrentHashMap<>();

    @Override
    public PagamentoGateway.Resposta processar(
            PagamentoGateway.Solicitacao solicitacao) {

        GatewayRequest requisicao = GatewayRequest.from(
                solicitacao.pedidoId(),
                solicitacao.valor(),
                solicitacao.token(),
                solicitacao.forma(),
                solicitacao.regiao(),
                solicitacao.origem(),
                solicitacao.destino()
        );

        GatewayResult resultado = processarPagamento(requisicao);

        return new PagamentoGateway.Resposta(
                resultado.transactionCode(),
                resultado.providerStatus(),
                resultado.providerMessage()
        );
    }

    public GatewayResult processarPagamento(GatewayRequest request) {

        boolean rejeitado =
                "REJEITADO".equalsIgnoreCase(request.paymentToken());

        String codigo = "FN-" + request.orderReference();

        String status = rejeitado ? "DECLINED" : "AUTHORIZED";

        String mensagem = rejeitado
                ? "Pagamento rejeitado pelo provedor simulado"
                : "Pagamento autorizado";

        GatewayResult resultado =
                new GatewayResult(codigo, status, mensagem);

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
                Long pedidoId,
                BigDecimal valor,
                String token,
                String forma) {

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
                String destino) {

            long valorCentavos =
                    valor.movePointRight(2).longValueExact();

            return new GatewayRequest(
                    pedidoId.toString(),
                    valorCentavos,
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
