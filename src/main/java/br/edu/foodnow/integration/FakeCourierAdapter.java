package br.edu.foodnow.integration;

import br.edu.foodnow.entregadores.ProvedorEntregadores;
import org.springframework.stereotype.Component;

/** Traduz o contrato de despacho para o simulador existente, sem alterar sua lógica. */
@Component
public class FakeCourierAdapter implements ProvedorEntregadores {
    private final FakeCourierClient cliente;

    public FakeCourierAdapter(FakeCourierClient cliente) {
        this.cliente = cliente;
    }

    @Override
    public ResultadoDespacho solicitarEntregador(SolicitacaoDespacho solicitacao) {
        var requisicao = new FakeCourierClient.CourierRequest(
                solicitacao.pedidoId(), solicitacao.distanciaKm(), solicitacao.zona(),
                solicitacao.coordenadasDestino());
        var resultado = cliente.solicitarEntregador(requisicao);
        return new ResultadoDespacho(resultado.courierCode(), resultado.providerStatus(),
                resultado.pickupEtaMinutes());
    }
}
