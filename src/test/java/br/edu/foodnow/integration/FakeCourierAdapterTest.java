package br.edu.foodnow.integration;

import br.edu.foodnow.entregadores.ProvedorEntregadores;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class FakeCourierAdapterTest {
    private final ProvedorEntregadores provedor = new FakeCourierAdapter(new FakeCourierClient());

    @Test
    void devePreservarDespachoCentralComTempoDeColeta() {
        var resposta = provedor.solicitarEntregador(
                new ProvedorEntregadores.SolicitacaoDespacho(42L, 3.0, "CENTRAL", "-25,-50"));
        assertThat(resposta.codigoEntregador()).isEqualTo("COURIER-42");
        assertThat(resposta.statusProvedor()).isEqualTo("DRIVER_ASSIGNED");
        assertThat(resposta.minutosAteColeta()).isEqualTo(4);
    }

    @Test
    void devePreservarTempoDeColetaForaDaZonaCentral() {
        var resposta = provedor.solicitarEntregador(
                new ProvedorEntregadores.SolicitacaoDespacho(43L, 5.0, "URBANA", "-25,-50"));
        assertThat(resposta.codigoEntregador()).isEqualTo("COURIER-43");
        assertThat(resposta.statusProvedor()).isEqualTo("DRIVER_ASSIGNED");
        assertThat(resposta.minutosAteColeta()).isEqualTo(9);
    }

    @Test
    void devePreservarAusenciaDeEntregadorPorDistanciaOuZona() {
        for (var solicitacao : new ProvedorEntregadores.SolicitacaoDespacho[] {
                new ProvedorEntregadores.SolicitacaoDespacho(1L, 40.01, "CENTRAL", "-25,-50"),
                new ProvedorEntregadores.SolicitacaoDespacho(2L, 2.0, "EXTERNA", "-25,-50") }) {
            var resposta = provedor.solicitarEntregador(solicitacao);
            assertThat(resposta.codigoEntregador()).isNull();
            assertThat(resposta.statusProvedor()).isEqualTo("NO_COURIER_AVAILABLE");
            assertThat(resposta.minutosAteColeta()).isZero();
        }
    }
}
