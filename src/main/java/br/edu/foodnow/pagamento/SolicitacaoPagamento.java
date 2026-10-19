package br.edu.foodnow.pagamento;

public record SolicitacaoPagamento(
        String orderReference,
        long amountInCents,
        String paymentToken,
        String paymentMethod,
        String deliveryZone,
        String originCoordinates,
        String destinationCoordinates
) {
}
