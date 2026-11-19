package br.edu.foodnow.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class FakePaymentGatewayResilienceTest {

    @Test
    void deveReutilizarResultadoERegistrarQuandoPagamentoChegaDuplicado(
            CapturedOutput output
    ) {

        FakePaymentGateway gateway = new FakePaymentGateway();

        var requisicao = FakePaymentGateway.GatewayRequest.from(
                42L,
                new BigDecimal("59.90"),
                "TOKEN_CONFIDENCIAL",
                "PIX"
        );

        // Primeira chamada: processa o pagamento.
        var primeiroResultado = gateway.processarPagamento(requisicao);

        // Segunda chamada: simula uma requisição duplicada.
        var resultadoDuplicado = gateway.processarPagamento(requisicao);

        // Verifica o resultado do primeiro processamento.
        assertThat(primeiroResultado.providerStatus())
                .isEqualTo("AUTHORIZED");

        // Verifica que a segunda chamada reutilizou o mesmo resultado.
        assertThat(resultadoDuplicado)
                .isSameAs(primeiroResultado);

        // Verifica que a transação continua consultável.
        assertThat(gateway.consultarPagamento("FN-42"))
                .isSameAs(primeiroResultado);

        // Verifica os registros de diagnóstico.
        assertThat(output.getAll())
                .contains("operacao=PROCESSAR_PAGAMENTO")
                .contains("pedidoId=42")
                .contains("resultado=AUTHORIZED")
                .contains("tipoFalha=REQUISICAO_DUPLICADA")
                .contains("tratamento=RESULTADO_ANTERIOR_REUTILIZADO")
                .contains("codigoTransacao=FN-42")
                .doesNotContain("TOKEN_CONFIDENCIAL");
    }
}