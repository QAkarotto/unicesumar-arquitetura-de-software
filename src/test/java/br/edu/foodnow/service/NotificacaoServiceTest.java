package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.logistica.PoliticaEntregaAtual;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.notificacao.CanalNotificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Testa as regras de composição das notificações usando somente o contrato
 * CanalNotificacao, sem depender de FakeEmailClient.
 */
class NotificacaoServiceTest {

    private final CanalNotificacaoEmMemoria canal =
            new CanalNotificacaoEmMemoria();

    private final LocalizacaoService localizacaoService =
            mock(LocalizacaoService.class);

    private final LogisticaService logisticaService =
            mock(LogisticaService.class);

    private final NotificacaoService service =
            new NotificacaoService(
                    canal,
                    localizacaoService,
                    logisticaService
            );

    private Pedido pedido;

    @BeforeEach
    void prepararPedido() {
        Endereco origem = endereco(
                "Rua do Restaurante", "Centro", -25.100, -50.150
        );

        Endereco destino = endereco(
                "Rua do Cliente", "Centro", -25.095, -50.160
        );

        Cliente cliente = new Cliente(
                "Ana", "ana@foodnow.test"
        );

        cliente.adicionarEndereco(destino);

        Restaurante restaurante = new Restaurante(
                "Cantina", 20, origem
        );

        pedido = new Pedido(cliente, restaurante, destino);

        ReflectionTestUtils.setField(pedido, "id", 42L);
    }

    @Test
    void deveEnviarConfirmacaoPeloContratoDaAplicacao() {
        when(logisticaService.resumirPedido(pedido))
                .thenReturn(
                        new PoliticaEntregaAtual.ResumoPedido(
                                "CENTRAL", 25
                        )
                );

        service.notificarConfirmacao(pedido);

        Notificacao enviada = unicaNotificacao();

        assertThat(enviada.getDestinatario())
                .isEqualTo("ana@foodnow.test");

        assertThat(enviada.getAssunto())
                .isEqualTo("Pedido confirmado");

        assertThat(enviada.getMensagem())
                .contains("pedido 42", "região CENTRAL", "25 minutos");
    }

    @Test
    void deveEnviarNotificacaoDeInicioDeEntregaPeloContratoDaAplicacao() {
        service.notificarInicioEntrega(pedido);

        Notificacao enviada = unicaNotificacao();

        assertThat(enviada.getDestinatario())
                .isEqualTo("ana@foodnow.test");

        assertThat(enviada.getAssunto())
                .isEqualTo("Entrega iniciada");

        assertThat(enviada.getMensagem())
                .isEqualTo("O pedido 42 saiu para entrega.");
    }

    @Test
    void deveEnviarNotificacaoDePagamentoAprovadoComReferenciaGeografica() {
        when(localizacaoService.calcularRota(
                pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega()
        )).thenReturn(new Rota(1.5, 12, "CENTRAL"));

        service.notificarPagamento(
                pedido, StatusPagamento.APROVADO
        );

        Notificacao enviada = unicaNotificacao();

        assertThat(enviada.getAssunto())
                .isEqualTo("Pagamento aprovado");

        assertThat(enviada.getMensagem())
                .contains(
                        "pedido 42",
                        "12 minutos",
                        "CENTRAL",
                        "-25.095000,-50.160000"
                );
    }

    @Test
    void deveEnviarNotificacaoDePagamentoRejeitadoComReferenciaGeografica() {
        when(localizacaoService.calcularRota(
                pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega()
        )).thenReturn(new Rota(1.5, 12, "CENTRAL"));

        service.notificarPagamento(
                pedido, StatusPagamento.REJEITADO
        );

        Notificacao enviada = unicaNotificacao();

        assertThat(enviada.getAssunto())
                .isEqualTo("Pagamento rejeitado");

        assertThat(enviada.getMensagem())
                .contains(
                        "pedido 42",
                        "rejeitado na zona CENTRAL",
                        "-25.095000,-50.160000"
                );
    }

    private Notificacao unicaNotificacao() {
        assertThat(canal.enviadas).hasSize(1);
        return canal.enviadas.getFirst();
    }

    private Endereco endereco(
            String logradouro,
            String bairro,
            double latitude,
            double longitude
    ) {
        return new Endereco(
                logradouro,
                "10",
                bairro,
                "Ponta Grossa",
                "84000-000",
                new Localizacao(latitude, longitude)
        );
    }

    /**
     * Implementação de teste do contrato da aplicação.
     * Não conhece o cliente de e-mail.
     */
    private static class CanalNotificacaoEmMemoria
            implements CanalNotificacao {

        private final List<Notificacao> enviadas =
                new ArrayList<>();

        @Override
        public void enviar(Notificacao notificacao) {
            enviadas.add(notificacao);
        }
    }
}