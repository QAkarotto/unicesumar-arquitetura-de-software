package br.edu.foodnow.entrega;

/**
 * Plano de despacho de uma entrega: o que a {@link PoliticaEntrega} decide antes de
 * chamar o serviço de entregadores.
 *
 * @param distanciaKm      distância enviada ao entregador e gravada na entrega
 * @param zona             zona de despacho
 * @param tempoBaseMinutos tempo estimado sem o tempo de coleta do entregador
 */
public record PlanoEntrega(double distanciaKm, String zona, int tempoBaseMinutos) {

    public int tempoTotalMinutos(int esperaColetaMinutos) {
        return tempoBaseMinutos + esperaColetaMinutos;
    }
}
