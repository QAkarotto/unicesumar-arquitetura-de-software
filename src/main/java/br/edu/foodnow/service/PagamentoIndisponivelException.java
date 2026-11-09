package br.edu.foodnow.service;

public class PagamentoIndisponivelException extends RuntimeException {
    public PagamentoIndisponivelException(Throwable causa) {
        super("Não foi possível confirmar o pagamento com o provedor. Tente novamente em instantes.", causa);
    }
}
