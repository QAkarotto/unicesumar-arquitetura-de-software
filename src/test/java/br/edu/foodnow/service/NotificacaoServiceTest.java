package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusPagamento;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificacaoServiceTest {
    private final LocalizacaoService localizacaoService = mock(LocalizacaoService.class);
    private final LogisticaService logisticaService = mock(LogisticaService.class);

    @Test
    void deveEnviarNaPrimeiraTentativaQuandoServicoResponde() {
        EmailInstavel email = new EmailInstavel(0);

        servico(email).notificarInicioEntrega(pedido());

        assertThat(email.tentativas).isEqualTo(1);
        assertThat(email.getEnviadas()).extracting(Notificacao::getAssunto).containsExactly("Entrega iniciada");
    }

    @Test
    void deveTentarNovamenteQuandoServicoFalhaTemporariamente() {
        EmailInstavel email = new EmailInstavel(2);

        servico(email).notificarInicioEntrega(pedido());

        assertThat(email.tentativas).isEqualTo(3);
        assertThat(email.getEnviadas()).hasSize(1);
    }

    @Test
    void naoDeveInterromperOperacaoQuandoServicoContinuaIndisponivel() {
        EmailInstavel email = new EmailInstavel(Integer.MAX_VALUE);

        assertThatCode(() -> servico(email).notificarInicioEntrega(pedido())).doesNotThrowAnyException();

        assertThat(email.tentativas).isEqualTo(NotificacaoService.MAXIMO_TENTATIVAS);
        assertThat(email.getEnviadas()).isEmpty();
    }

    @Test
    void falhaNaNotificacaoDePagamentoNaoDeveSerPropagada() {
        EmailInstavel email = new EmailInstavel(Integer.MAX_VALUE);
        when(localizacaoService.calcularRota(any(), any())).thenReturn(new Rota(1.5, 10, "CENTRAL"));

        assertThatCode(() -> servico(email).notificarPagamento(pedido(), StatusPagamento.APROVADO))
                .doesNotThrowAnyException();
        assertThat(email.tentativas).isEqualTo(NotificacaoService.MAXIMO_TENTATIVAS);
    }

    private NotificacaoService servico(FakeEmailClient email) {
        return new NotificacaoService(email, localizacaoService, logisticaService);
    }

    private Pedido pedido() {
        Endereco destino = new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.095, -50.160));
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(destino);
        Restaurante restaurante = new Restaurante("Cantina", 20, new Endereco("Rua B", "1", "Centro",
                "Ponta Grossa", "84000-000", new Localizacao(-25.100, -50.150)));
        return new Pedido(cliente, restaurante, destino);
    }

    /** Cliente de e-mail que falha nas primeiras chamadas, simulando indisponibilidade. */
    private static class EmailInstavel extends FakeEmailClient {
        private int falhasRestantes;
        private int tentativas;

        EmailInstavel(int falhas) {
            this.falhasRestantes = falhas;
        }

        @Override
        public void enviar(Notificacao notificacao) {
            tentativas++;
            if (falhasRestantes > 0) {
                falhasRestantes--;
                throw new IllegalStateException("Serviço de e-mail indisponível");
            }
            super.enviar(notificacao);
        }
    }
}
