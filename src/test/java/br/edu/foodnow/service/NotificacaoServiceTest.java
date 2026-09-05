package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusPagamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Caracteriza a fachada de notificação, que passou a ser o único caminho até o cliente de e-mail.
 * Os textos e a presença ou ausência da referência geográfica fazem parte do comportamento
 * observável verificado pelos testes de API.
 */
class NotificacaoServiceTest {
    private final FakeEmailClient emailClient = new FakeEmailClient();
    private final NotificacaoService notificacaoService = new NotificacaoService(emailClient);

    @Test
    void deveNotificarPedidoConfirmadoComRegiaoETempoDoProprioPedido() {
        Pedido pedido = pedidoComItem();

        notificacaoService.notificarPedidoConfirmado(pedido);

        assertThat(emailClient.getEnviadas()).singleElement().satisfies(notificacao -> {
            assertThat(notificacao.getDestinatario()).isEqualTo("ana@foodnow.test");
            assertThat(notificacao.getAssunto()).isEqualTo("Pedido confirmado");
            assertThat(notificacao.getMensagem())
                    .contains("foi confirmado para a região CENTRAL")
                    .contains("Estimativa interna: " + pedido.estimarTempoEntregaPeloPedido() + " minutos")
                    .doesNotContain("Região de entrega:");
        });
    }

    @Test
    void deveNotificarPagamentoAprovadoUsandoARotaRecebidaSemConsultarOMapa() {
        Pedido pedido = pedidoComItem();

        notificacaoService.notificarPagamento(pedido, StatusPagamento.APROVADO,
                new Rota(1.33, 17, "CENTRAL"));

        assertThat(emailClient.getEnviadas()).singleElement().satisfies(notificacao -> {
            assertThat(notificacao.getAssunto()).isEqualTo("Pagamento aprovado");
            assertThat(notificacao.getMensagem())
                    .contains("Rota estimada pelo e-mail: 17 minutos")
                    .contains("Região de entrega: CENTRAL [-25.095000,-50.160000]");
        });
    }

    @Test
    void deveNotificarPagamentoRejeitadoComAZonaDaRotaRecebida() {
        Pedido pedido = pedidoComItem();

        notificacaoService.notificarPagamento(pedido, StatusPagamento.REJEITADO,
                new Rota(9.9, 45, "EXPANDIDA"));

        assertThat(emailClient.getEnviadas()).singleElement().satisfies(notificacao -> {
            assertThat(notificacao.getAssunto()).isEqualTo("Pagamento rejeitado");
            assertThat(notificacao.getMensagem())
                    .contains("foi rejeitado na zona EXPANDIDA")
                    .contains("Região de entrega: CENTRAL");
        });
    }

    @Test
    void deveNotificarEntregaIniciadaSemReferenciaGeografica() {
        notificacaoService.notificarEntregaIniciada(pedidoComItem());

        assertThat(emailClient.getEnviadas()).singleElement().satisfies(notificacao -> {
            assertThat(notificacao.getAssunto()).isEqualTo("Entrega iniciada");
            assertThat(notificacao.getMensagem()).contains("saiu para entrega")
                    .doesNotContain("Região de entrega:");
        });
    }

    private Pedido pedidoComItem() {
        Endereco destino = new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.095, -50.160));
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(destino);
        Restaurante restaurante = new Restaurante("Restaurante", 20,
                new Endereco("Rua Cantina", "1", "Centro", "Ponta Grossa", "84000-000",
                        new Localizacao(-25.100, -50.150)));
        Pedido pedido = new Pedido(cliente, restaurante, destino);
        pedido.adicionarItem(new Produto("Pizza", new BigDecimal("25.00"), true, restaurante), 2);
        return pedido;
    }
}
