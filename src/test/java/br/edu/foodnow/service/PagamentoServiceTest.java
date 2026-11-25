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
import br.edu.foodnow.pagamento.ProcessadorPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

/** Exercita o caso de uso de pagamento com um processador falso, sem o FakePaymentGateway. */
class PagamentoServiceTest {
    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final PagamentoRepository pagamentoRepository = mock(PagamentoRepository.class);
    private final NotificacaoService notificacaoService = mock(NotificacaoService.class);
    private final EntregaService entregaService = mock(EntregaService.class);
    private final LocalizacaoService localizacaoService = mock(LocalizacaoService.class);
    private final LogisticaService logisticaService = mock(LogisticaService.class);
    private final ProcessadorFalso processador = new ProcessadorFalso();
    private final PagamentoService service = new PagamentoService(pedidoRepository, pagamentoRepository,
            processador, notificacaoService, entregaService, localizacaoService, logisticaService);
    private Pedido pedido;

    @BeforeEach
    void prepararPedidoConfirmado() {
        pedido = pedidoConfirmado();
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(localizacaoService.calcularRota(any(), any())).thenReturn(new Rota(1.32, 17, "CENTRAL"));
        when(logisticaService.resumirPedido(pedido)).thenReturn(new PoliticaEntregaAtual.ResumoPedido("CENTRAL", 21));
    }

    @Test
    void deveRegistrarPagamentoAprovadoComODadoDevolvidoPeloProcessador() {
        processador.responder(new ResultadoPagamento(StatusPagamento.APROVADO, "TX-77", "Aprovado no teste"));

        Pagamento pagamento = service.processar(1L, FormaPagamento.PIX, "qualquer-token");

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(pagamento.getCodigoExterno()).isEqualTo("TX-77");
        assertThat(pagamento.getMensagemProvedor()).isEqualTo("Aprovado no teste");
        assertThat(pagamento.getValor()).isEqualByComparingTo("56.55");
        assertThat(pagamento.getRegiaoEntrega()).isEqualTo("CENTRAL");
        assertThat(pagamento.getDistanciaValidadaKm()).isEqualTo(1.32);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        verify(pedidoRepository).save(pedido);
        verify(notificacaoService).notificarPagamento(pedido, StatusPagamento.APROVADO);
        verify(entregaService).criarPara(pedido);
    }

    @Test
    void deveRegistrarRejeicaoSemCriarEntrega() {
        processador.responder(new ResultadoPagamento(StatusPagamento.REJEITADO, "TX-78", "Recusado no teste"));

        Pagamento pagamento = service.processar(1L, FormaPagamento.CARTAO_CREDITO, "qualquer-token");

        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGAMENTO_REJEITADO);
        verify(notificacaoService).notificarPagamento(pedido, StatusPagamento.REJEITADO);
        verify(entregaService, never()).criarPara(any());
    }

    @Test
    void deveEnviarAoProcessadorOsDadosDoPedidoNoVocabularioDaAplicacao() {
        processador.responder(new ResultadoPagamento(StatusPagamento.APROVADO, "TX-79", "ok"));

        service.processar(1L, FormaPagamento.PIX, "TOKEN_OK");

        assertThat(processador.solicitacoes).singleElement().satisfies(solicitacao -> {
            assertThat(solicitacao.valor()).isEqualByComparingTo("56.55");
            assertThat(solicitacao.forma()).isEqualTo(FormaPagamento.PIX);
            assertThat(solicitacao.token()).isEqualTo("TOKEN_OK");
            assertThat(solicitacao.regiaoEntrega()).isEqualTo("CENTRAL");
            assertThat(solicitacao.origem()).isSameAs(pedido.getRestaurante().getEndereco().getLocalizacao());
            assertThat(solicitacao.destino()).isSameAs(pedido.getEnderecoEntrega().getLocalizacao());
        });
    }

    @Test
    void naoDeveChamarProcessadorParaPedidoNaoConfirmado() {
        Pedido naoConfirmado = pedidoComItem();
        when(pedidoRepository.findById(2L)).thenReturn(Optional.of(naoConfirmado));

        assertThatThrownBy(() -> service.processar(2L, FormaPagamento.PIX, "TOKEN_OK"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Pedido precisa estar confirmado para pagamento");
        assertThat(processador.solicitacoes).isEmpty();
    }

    @Test
    void deveRejeitarPedidoInexistenteSemChamarProcessador() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processar(99L, FormaPagamento.PIX, "TOKEN_OK"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pedido não encontrado");
        assertThat(processador.solicitacoes).isEmpty();
    }

    private Pedido pedidoConfirmado() {
        Pedido confirmado = pedidoComItem();
        confirmado.confirmar();
        return confirmado;
    }

    private Pedido pedidoComItem() {
        Endereco destino = enderecoEm(-25.095, -50.160);
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(destino);
        Restaurante restaurante = new Restaurante("Cantina", 20, enderecoEm(-25.100, -50.150));
        Pedido novo = new Pedido(cliente, restaurante, destino);
        novo.adicionarItem(new Produto("Pizza", new BigDecimal("25.00"), true, restaurante), 2);
        novo.definirEntrega(1.32, new BigDecimal("6.55"));
        return novo;
    }

    private Endereco enderecoEm(double latitude, double longitude) {
        return new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(latitude, longitude));
    }

    private static class ProcessadorFalso implements ProcessadorPagamento {
        private final List<SolicitacaoPagamento> solicitacoes = new ArrayList<>();
        private ResultadoPagamento resposta;

        void responder(ResultadoPagamento resultado) {
            this.resposta = resultado;
        }

        @Override
        public ResultadoPagamento processar(SolicitacaoPagamento solicitacao) {
            solicitacoes.add(solicitacao);
            return resposta;
        }
    }
}
