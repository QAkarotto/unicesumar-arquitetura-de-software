package br.edu.foodnow.service;

import br.edu.foodnow.integration.EmailIndisponivelException;
import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.integration.MapsLocalizacaoAdapter;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.logistica.PoliticaEntregaAtual;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacaoServiceTest {
    @ParameterizedTest
    @ValueSource(strings = {"Pedido confirmado", "Entrega iniciada", "Pagamento aprovado", "Pagamento rejeitado"})
    void deveEnviarUmaNotificacaoComConteudoPreservado(String assunto) {
        ClienteControlado cliente = new ClienteControlado();
        notificar(service(cliente), assunto);

        assertThat(cliente.tentativas).isEqualTo(1);
        assertThat(cliente.getEnviadas()).singleElement().satisfies(notificacao -> {
            assertThat(notificacao.getAssunto()).isEqualTo(assunto);
            assertThat(notificacao.getDestinatario()).isEqualTo("cliente@foodnow.test");
            assertThat(notificacao.getMensagem()).contains("pedido");
            if (assunto.startsWith("Pagamento")) {
                assertThat(notificacao.getMensagem()).contains("Região de entrega: CENTRAL");
            }
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pedido confirmado", "Entrega iniciada", "Pagamento aprovado", "Pagamento rejeitado"})
    void deveConterIndisponibilidadeSemReenviar(String assunto) {
        ClienteControlado cliente = new ClienteControlado();
        cliente.falha = new EmailIndisponivelException("Provedor indisponível");

        assertThatCode(() -> notificar(service(cliente), assunto)).doesNotThrowAnyException();

        assertThat(cliente.tentativas).isEqualTo(1);
        assertThat(cliente.getEnviadas()).isEmpty();
    }

    @Test
    void naoDeveOcultarExcecoesInesperadas() {
        ClienteControlado cliente = new ClienteControlado();
        cliente.falha = new IllegalStateException("Defeito inesperado");

        assertThatThrownBy(() -> service(cliente).notificarInicioEntrega(pedido()))
                .isSameAs(cliente.falha);
        assertThat(cliente.tentativas).isEqualTo(1);
    }

    @Test
    void naoDeveDuplicarQuandoProvedorAceitaAntesDeSinalizarFalha() {
        ClienteControlado cliente = new ClienteControlado();
        cliente.registrarAntesDeFalhar = true;
        cliente.falha = new EmailIndisponivelException("Resposta perdida após aceitação");

        assertThatCode(() -> service(cliente).notificarInicioEntrega(pedido())).doesNotThrowAnyException();

        assertThat(cliente.tentativas).isEqualTo(1);
        assertThat(cliente.getEnviadas()).hasSize(1);
    }

    private NotificacaoService service(FakeEmailClient cliente) {
        var provedor = new MapsLocalizacaoAdapter(new FakeMapsClient());
        return new NotificacaoService(cliente, new LocalizacaoService(provedor),
                new LogisticaService(provedor, new PoliticaEntregaAtual()));
    }

    private void notificar(NotificacaoService service, String assunto) {
        switch (assunto) {
            case "Pedido confirmado" -> service.notificarConfirmacao(pedido());
            case "Entrega iniciada" -> service.notificarInicioEntrega(pedido());
            case "Pagamento aprovado" -> service.notificarPagamento(pedido(), StatusPagamento.APROVADO);
            case "Pagamento rejeitado" -> service.notificarPagamento(pedido(), StatusPagamento.REJEITADO);
            default -> throw new IllegalArgumentException(assunto);
        }
    }

    private Pedido pedido() {
        Endereco endereco = new Endereco("Rua", "1", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.100, -50.150));
        Cliente cliente = new Cliente("Cliente", "cliente@foodnow.test");
        cliente.adicionarEndereco(endereco);
        return new Pedido(cliente, new Restaurante("Cantina", 25, endereco), endereco);
    }

    private static class ClienteControlado extends FakeEmailClient {
        private int tentativas;
        private RuntimeException falha;
        private boolean registrarAntesDeFalhar;

        @Override
        public void enviar(Notificacao notificacao) {
            tentativas++;
            if (registrarAntesDeFalhar) {
                super.enviar(notificacao);
            }
            if (falha != null) {
                throw falha;
            }
            if (!registrarAntesDeFalhar) {
                super.enviar(notificacao);
            }
        }
    }
}
