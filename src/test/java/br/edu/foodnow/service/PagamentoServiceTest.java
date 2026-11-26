package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.logistica.PoliticaEntregaAtual;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.PagamentoProcessado;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa a regra de pagamento usando apenas o contrato {@link GatewayPagamento},
 * sem depender do provedor simulado ({@code FakePaymentGateway}).
 */
class PagamentoServiceTest {
    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final PagamentoRepository pagamentoRepository = mock(PagamentoRepository.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    private final EntregaService entregaService = mock(EntregaService.class);
    private final LocalizacaoService localizacaoService = mock(LocalizacaoService.class);
    private final LogisticaService logisticaService = mock(LogisticaService.class);
    private final GatewayPagamentoEmMemoria gateway = new GatewayPagamentoEmMemoria();

    private PagamentoService pagamentoService;
    private Pedido pedido;

    @BeforeEach
    void configurar() {
        pagamentoService = new PagamentoService(pedidoRepository, pagamentoRepository, gateway,
                entregaService, localizacaoService, logisticaService, eventos);
        pedido = pedidoComItem();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        when(localizacaoService.calcularRota(any(), any())).thenReturn(new Rota(2.5, 12, "CENTRAL"));
        when(logisticaService.resumirPedido(pedido)).thenReturn(new PoliticaEntregaAtual.ResumoPedido("CENTRAL", 30));
    }

    @Test
    void deveRegistrarPagamentoAprovadoECriarEntrega() {
        pedido.confirmar();
        gateway.responderCom(new ResultadoPagamento(StatusPagamento.APROVADO, "TX-1", "Pagamento autorizado"));

        Pagamento pagamento = pagamentoService.processar(1L, FormaPagamento.PIX, "TOKEN");

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(pagamento.getCodigoExterno()).isEqualTo("TX-1");
        assertThat(pagamento.getMensagemProvedor()).isEqualTo("Pagamento autorizado");
        assertThat(pagamento.getValor()).isEqualByComparingTo(pedido.getValorTotal());
        assertThat(pagamento.getRegiaoEntrega()).isEqualTo("CENTRAL");
        assertThat(pagamento.getDistanciaValidadaKm()).isEqualTo(2.5);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        verify(eventos).publishEvent(new PagamentoProcessado(null, null, StatusPagamento.APROVADO));
        verify(entregaService).criarPara(pedido);
    }

    @Test
    void deveEnviarAoGatewaySomenteDadosDoDominio() {
        pedido.confirmar();
        gateway.responderCom(new ResultadoPagamento(StatusPagamento.APROVADO, "TX-2", "ok"));

        pagamentoService.processar(1L, FormaPagamento.CARTAO_CREDITO, "TOKEN_CARTAO");

        assertThat(gateway.solicitacoes).singleElement().satisfies(solicitacao -> {
            assertThat(solicitacao.valor()).isEqualByComparingTo(pedido.getValorTotal());
            assertThat(solicitacao.token()).isEqualTo("TOKEN_CARTAO");
            assertThat(solicitacao.forma()).isEqualTo(FormaPagamento.CARTAO_CREDITO);
            assertThat(solicitacao.regiaoEntrega()).isEqualTo("CENTRAL");
            assertThat(solicitacao.origem()).isSameAs(pedido.getRestaurante().getEndereco().getLocalizacao());
            assertThat(solicitacao.destino()).isSameAs(pedido.getEnderecoEntrega().getLocalizacao());
        });
    }

    @Test
    void deveRegistrarPagamentoRejeitadoSemCriarEntrega() {
        pedido.confirmar();
        gateway.responderCom(new ResultadoPagamento(StatusPagamento.REJEITADO, "TX-3", "Recusado"));

        Pagamento pagamento = pagamentoService.processar(1L, FormaPagamento.PIX, "QUALQUER");

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGAMENTO_REJEITADO);
        verify(eventos).publishEvent(new PagamentoProcessado(null, null, StatusPagamento.REJEITADO));
        verify(entregaService, never()).criarPara(any());
    }

    @Test
    void naoDeveAcionarGatewayParaPedidoNaoConfirmado() {
        assertThatThrownBy(() -> pagamentoService.processar(1L, FormaPagamento.PIX, "TOKEN"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Pedido precisa estar confirmado para pagamento");

        assertThat(gateway.solicitacoes).isEmpty();
        verify(pagamentoRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any());
    }

    @Test
    void deveInformarPedidoInexistente() {
        assertThatThrownBy(() -> pagamentoService.processar(99L, FormaPagamento.PIX, "TOKEN"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(gateway.solicitacoes).isEmpty();
    }

    private Pedido pedidoComItem() {
        Endereco enderecoCliente = new Endereco("Rua Cliente", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.095, -50.160));
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(enderecoCliente);
        Restaurante restaurante = new Restaurante("Cantina", 20, new Endereco("Rua Cantina", "1", "Centro",
                "Ponta Grossa", "84000-000", new Localizacao(-25.100, -50.150)));
        Pedido novo = new Pedido(cliente, restaurante, enderecoCliente);
        novo.adicionarItem(new Produto("Pizza", new BigDecimal("30.00"), true, restaurante), 2);
        novo.definirEntrega(2.5, new BigDecimal("6.55"));
        return novo;
    }

    /** Implementação de teste do contrato: registra as solicitações e devolve uma resposta pré-definida. */
    private static final class GatewayPagamentoEmMemoria implements GatewayPagamento {
        private final List<SolicitacaoPagamento> solicitacoes = new ArrayList<>();
        private ResultadoPagamento resposta;

        void responderCom(ResultadoPagamento resposta) {
            this.resposta = resposta;
        }

        @Override
        public ResultadoPagamento autorizar(SolicitacaoPagamento solicitacao) {
            solicitacoes.add(solicitacao);
            return resposta;
        }
    }
}
