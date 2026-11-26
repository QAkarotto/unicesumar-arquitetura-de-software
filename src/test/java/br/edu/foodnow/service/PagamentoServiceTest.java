package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.logistica.PoliticaEntregaAtual;
import br.edu.foodnow.model.*;
import br.edu.foodnow.pagamento.AutorizadorPagamento;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PagamentoServiceTest {
    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final PagamentoRepository pagamentos = mock(PagamentoRepository.class);
    private final AutorizadorPagamento autorizador = mock(AutorizadorPagamento.class);
    private final NotificacaoService notificacoes = mock(NotificacaoService.class);
    private final EntregaService entregas = mock(EntregaService.class);
    private final LocalizacaoService localizacao = mock(LocalizacaoService.class);
    private final LogisticaService logistica = mock(LogisticaService.class);
    private final PagamentoService service = new PagamentoService(pedidos, pagamentos, autorizador,
            notificacoes, entregas, localizacao, logistica);

    @ParameterizedTest
    @EnumSource(StatusPagamento.class)
    void deveAplicarResultadoDoContratoSemConhecerProvedor(StatusPagamento status) {
        Pedido pedido = pedido();
        pedido.confirmar();
        when(pedidos.findById(15L)).thenReturn(Optional.of(pedido));
        when(localizacao.calcularRota(pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega()))
                .thenReturn(new Rota(3.0, 20, "CENTRAL"));
        when(logistica.resumirPedido(pedido)).thenReturn(new PoliticaEntregaAtual.ResumoPedido("CENTRAL", 20));
        when(autorizador.autorizar(any())).thenReturn(
                new AutorizadorPagamento.Resultado(status, "TRANSACAO-15", "Resposta de teste"));
        when(pagamentos.save(any(Pagamento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pagamento pagamento = service.processar(15L, FormaPagamento.PIX, "token-do-teste");

        assertThat(pagamento.getStatus()).isEqualTo(status);
        assertThat(pagamento.getCodigoExterno()).isEqualTo("TRANSACAO-15");
        assertThat(pagamento.getMensagemProvedor()).isEqualTo("Resposta de teste");
        assertThat(pagamento.getValor()).isEqualByComparingTo("55.00");
        assertThat(pagamento.getForma()).isEqualTo(FormaPagamento.PIX);
        assertThat(pagamento.getPedido()).isSameAs(pedido);
        assertThat(pagamento.getRegiaoEntrega()).isEqualTo("CENTRAL");
        assertThat(pagamento.getDistanciaValidadaKm()).isEqualTo(3.0);
        assertThat(pedido.getStatus()).isEqualTo(status == StatusPagamento.APROVADO
                ? StatusPedido.PAGO : StatusPedido.PAGAMENTO_REJEITADO);
        verify(autorizador).autorizar(new AutorizadorPagamento.Solicitacao(15L,
                pedido.getValorTotal(), FormaPagamento.PIX, "token-do-teste", "CENTRAL",
                pedido.getRestaurante().getEndereco().getLocalizacao(),
                pedido.getEnderecoEntrega().getLocalizacao()));
        var ordem = inOrder(pedidos, pagamentos, notificacoes, entregas);
        ordem.verify(pedidos).save(pedido);
        ordem.verify(pagamentos).save(pagamento);
        ordem.verify(notificacoes).notificarPagamento(pedido, status);
        if (status == StatusPagamento.APROVADO) {
            ordem.verify(entregas).criarPara(pedido);
        } else {
            verifyNoInteractions(entregas);
        }
    }

    @Test
    void naoDeveAutorizarPedidoNaoConfirmado() {
        when(pedidos.findById(15L)).thenReturn(Optional.of(pedido()));
        assertThatThrownBy(() -> service.processar(15L, FormaPagamento.PIX, "token"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Pedido precisa estar confirmado para pagamento");
        verifyNoInteractions(autorizador, pagamentos, notificacoes, entregas, localizacao, logistica);
    }

    @Test
    void naoDeveAutorizarPedidoInexistente() {
        when(pedidos.findById(15L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.processar(15L, FormaPagamento.PIX, "token"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pedido não encontrado");
        verifyNoInteractions(autorizador, pagamentos, notificacoes, entregas, localizacao, logistica);
    }

    private Pedido pedido() {
        Endereco origem = endereco(-25.100, -50.150);
        Endereco destino = endereco(-25.095, -50.160);
        Restaurante restaurante = new Restaurante("Cantina", 25, origem);
        Cliente cliente = new Cliente("Cliente", "cliente@foodnow.test");
        Pedido pedido = new Pedido(cliente, restaurante, destino);
        ReflectionTestUtils.setField(pedido, "id", 15L);
        pedido.adicionarItem(new Produto("Pizza", new BigDecimal("25.00"), true, restaurante), 2);
        pedido.definirEntrega(3, new BigDecimal("5.00"));
        return pedido;
    }

    private Endereco endereco(double latitude, double longitude) {
        return new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(latitude, longitude));
    }
}
