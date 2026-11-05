package br.edu.foodnow.entregadores;

/** Contrato de despacho independente do fornecedor concreto. */
public interface ProvedorEntregadores {
    ResultadoDespacho solicitarEntregador(SolicitacaoDespacho solicitacao);

    record SolicitacaoDespacho(Long pedidoId, double distanciaKm, String zona,
                              String coordenadasDestino) { }

    record ResultadoDespacho(String codigoEntregador, String statusProvedor,
                             int minutosAteColeta) { }
}
