package br.edu.foodnow.integration;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IntegracoesSimuladasTest {

    @Test
    void mapasDevemRetornarCoordenadasInformadasERotaDeterministica() {
        FakeMapsClient client = new FakeMapsClient();
        Endereco endereco = enderecoCom(new Localizacao(-25.1, -50.15));
        FakeMapsClient.MapCoordinates coordenadas = client.buscarCoordenadas(endereco);
        FakeMapsClient.RouteResult primeira = client.calcularRota(
                new Localizacao(-25.1, -50.15), new Localizacao(-25.09, -50.16));
        FakeMapsClient.RouteResult segunda = client.calcularRota(
                new Localizacao(-25.1, -50.15), new Localizacao(-25.09, -50.16));

        assertThat(coordenadas.precision()).isEqualTo("EXACT");
        assertThat(Double.parseDouble(coordenadas.lat())).isEqualTo(-25.1);
        assertThat(primeira).isEqualTo(segunda);
        assertThat(primeira.deliveryZone()).isEqualTo("CENTRAL");
    }

    @Test
    void mapasDevemGerarCoordenadasQuandoAusentesEIdentificarZonaExpandida() {
        FakeMapsClient client = new FakeMapsClient();
        assertThat(client.buscarCoordenadas(enderecoCom(null)).precision()).isEqualTo("APPROXIMATE");
        assertThat(client.calcularRota(new Localizacao(-25, -50),
                new Localizacao(-25.2, -50.2)).deliveryZone()).isEqualTo("EXPANDIDA");
    }

    @Test
    void gatewayDeveAprovarEPermitirConsulta() {
        FakePaymentGateway gateway = new FakePaymentGateway();
        var request = FakePaymentGateway.GatewayRequest.from(10L, new BigDecimal("42.30"),
                "TOKEN_OK", "PIX");
        var resultado = gateway.processarPagamento(request);

        assertThat(request.amountInCents()).isEqualTo(4230);
        assertThat(resultado.providerStatus()).isEqualTo("AUTHORIZED");
        assertThat(gateway.consultarPagamento(resultado.transactionCode())).isEqualTo(resultado);
    }

    @Test
    void gatewayDeveRejeitarTokenReservado() {
        FakePaymentGateway gateway = new FakePaymentGateway();
        var resultado = gateway.processarPagamento(FakePaymentGateway.GatewayRequest.from(
                11L, BigDecimal.TEN, "REJEITADO", "CARTAO_CREDITO"));
        assertThat(resultado.providerStatus()).isEqualTo("DECLINED");
        assertThat(resultado.providerMessage()).contains("rejeitado");
    }

    @Test
    void emailDeveArmazenarNotificacoesEmMemoria() {
        FakeEmailClient client = new FakeEmailClient();
        client.enviar("aluno@foodnow.test", "Assunto", "Mensagem");
        assertThat(client.getEnviadas()).singleElement().satisfies(notificacao -> {
            assertThat(notificacao.getDestinatario()).isEqualTo("aluno@foodnow.test");
            assertThat(notificacao.getAssunto()).isEqualTo("Assunto");
            assertThat(notificacao.getMensagem()).isEqualTo("Mensagem");
        });
        client.limpar();
        assertThat(client.getEnviadas()).isEmpty();
    }

    private Endereco enderecoCom(Localizacao localizacao) {
        return new Endereco("Rua Integração", "1", "Centro", "Ponta Grossa", "84000-000", localizacao);
    }
}
