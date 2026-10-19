package br.edu.foodnow.pagamento;

public record ResultadoPagamento(
        String transactionCode,
        String providerStatus,
        String providerMessage
) {
}
