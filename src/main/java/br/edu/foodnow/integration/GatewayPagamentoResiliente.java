package br.edu.foodnow.integration;

import br.edu.foodnow.integration.FakePaymentGateway.GatewayRequest;
import br.edu.foodnow.integration.FakePaymentGateway.GatewayResult;
import br.edu.foodnow.service.PagamentoIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GatewayPagamentoResiliente {
    static final int MAX_TENTATIVAS = 3;
    private static final Logger log = LoggerFactory.getLogger(GatewayPagamentoResiliente.class);

    private final FakePaymentGateway gateway;
    private final Duration esperaBase;

    @Autowired
    public GatewayPagamentoResiliente(FakePaymentGateway gateway) {
        this(gateway, Duration.ofMillis(200));
    }

    GatewayPagamentoResiliente(FakePaymentGateway gateway, Duration esperaBase) {
        this.gateway = gateway;
        this.esperaBase = esperaBase;
    }

    // antes de repetir, consulta o provedor para nao cobrar duas vezes o mesmo pedido
    public GatewayResult processarPagamento(GatewayRequest request) {
        String pedido = request.orderReference();
        String codigo = FakePaymentGateway.codigoTransacao(pedido);
        for (int tentativa = 1; ; tentativa++) {
            try {
                GatewayResult resultado = gateway.processarPagamento(request);
                if (tentativa > 1) {
                    log.warn("operacao=processar_pagamento pedido={} tentativa={}/{} resultado=recuperado status={}",
                            pedido, tentativa, MAX_TENTATIVAS, resultado.providerStatus());
                }
                return resultado;
            } catch (RuntimeException falha) {
                // so o tipo da excecao, a mensagem do provedor pode trazer dados do pagamento
                log.warn("operacao=processar_pagamento pedido={} tentativa={}/{} falha={}",
                        pedido, tentativa, MAX_TENTATIVAS, falha.getClass().getSimpleName());

                GatewayResult existente = consultarTransacao(pedido, codigo, falha);
                if (existente != null) {
                    log.warn("operacao=processar_pagamento pedido={} resultado=reconciliado transacao={} status={}",
                            pedido, codigo, existente.providerStatus());
                    return existente;
                }
                if (tentativa == MAX_TENTATIVAS) {
                    log.error("operacao=processar_pagamento pedido={} resultado=tentativas_esgotadas tentativas={}",
                            pedido, MAX_TENTATIVAS);
                    throw new PagamentoIndisponivelException(falha);
                }
                esperar(tentativa, falha);
            }
        }
    }

    private GatewayResult consultarTransacao(String pedido, String codigo, RuntimeException falhaOriginal) {
        try {
            return gateway.consultarPagamento(codigo);
        } catch (RuntimeException falhaConsulta) {
            // sem saber se houve cobranca, repetir poderia duplicar o pagamento
            log.error("operacao=consultar_pagamento pedido={} transacao={} falha={} resultado=interrompido_estado_desconhecido",
                    pedido, codigo, falhaConsulta.getClass().getSimpleName());
            throw new PagamentoIndisponivelException(falhaOriginal);
        }
    }

    private void esperar(int tentativa, RuntimeException falha) {
        try {
            Thread.sleep(esperaBase.multipliedBy(tentativa));
        } catch (InterruptedException interrompido) {
            Thread.currentThread().interrupt();
            throw new PagamentoIndisponivelException(falha);
        }
    }
}
