package br.edu.foodnow.integration;

import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.GatewayPagamentoResiliente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PagamentoIntegracaoConfig {

    @Bean
    public GatewayPagamentoResiliente gatewayPagamento(FakePaymentGateway paymentGateway,
            @Value("${foodnow.pagamento.gateway.timeout:2s}") Duration timeout,
            @Value("${foodnow.pagamento.gateway.tentativas:3}") int tentativas,
            @Value("${foodnow.pagamento.gateway.espera-inicial:200ms}") Duration esperaInicial) {
        return new GatewayPagamentoResiliente(new FakePaymentGatewayAdapter(paymentGateway), timeout, tentativas,
                esperaInicial);
    }
}
