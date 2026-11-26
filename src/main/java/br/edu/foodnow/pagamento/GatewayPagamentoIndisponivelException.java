package br.edu.foodnow.pagamento;

/**
 * Falha transitória na comunicação com o provedor de pagamento (indisponibilidade ou timeout).
 * Diferente de uma recusa do pagamento, que é um resultado válido do provedor.
 */
public class GatewayPagamentoIndisponivelException extends RuntimeException {
    public GatewayPagamentoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
