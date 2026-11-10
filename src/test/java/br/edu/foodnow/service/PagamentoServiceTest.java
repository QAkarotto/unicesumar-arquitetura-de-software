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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testa a regra de pagamento sem o gateway simulado: o serviço recebe qualquer
 * implementação de {@link ProcessadorPagamento}.
 */
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
    void prepararPedido() {
        Endereco destino = endereco(-25.095, -50.160);
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(destino);
        Restaurante restaurante = new Restaurante("Cantina", 20, endereco(-25.100, -50.150));
        pedido = new Pedido(cliente, restaurante, destino);
        pedido.adicionarItem(new Produto("Pizza", new BigDecimal("30.00"), true, restaurante), 2);
        pedido.definirEntrega(1.5, new BigDecimal("6.55"));

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(localizacaoService.calcularRota(any(), any())).thenReturn(new Rota(1.5, 10, "CENTRAL"));
        when(logisticaService.resumirPedido(pedido)).thenReturn(new PoliticaEntregaAtual.ResumoPedido("CENTRAL", 21));
    }

    @Test
    void deveRegistrarPagamentoAprovadoECriarEntrega() {
        pedido.confirmar();
        processador.responder(new ResultadoPagamento(StatusPagamento.APROVADO, "TX-1", "ok"));

        Pagamento pagamento = service.processar(1L, FormaPagamento.PIX, "TOKEN");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(pagamento.getCodigoExterno()).isEqualTo("TX-1");
        assertThat(pagamento.getMensagemProvedor()).isEqualTo("ok");
        assertThat(pagamento.getValor()).isEqualByComparingTo("66.55");
        assertThat(pagamento.getRegiaoEntrega()).isEqualTo("CENTRAL");
        verify(notificacaoService).notificarPagamento(pedido, StatusPagamento.APROVADO);
        verify(entregaService).criarPara(pedido);
    }

    @Test
    void deveEnviarAoProcessadorOsDadosDoPedido() {
        pedido.confirmar();
        processador.responder(new ResultadoPagamento(StatusPagamento.APROVADO, "TX-1", "ok"));

        service.processar(1L, FormaPagamento.CARTAO_CREDITO, "TOKEN");

        SolicitacaoPagamento enviada = processador.ultimaSolicitacao;
        assertThat(enviada.valor()).isEqualByComparingTo("66.55");
        assertThat(enviada.forma()).isEqualTo(FormaPagamento.CARTAO_CREDITO);
        assertThat(enviada.token()).isEqualTo("TOKEN");
        assertThat(enviada.regiao()).isEqualTo("CENTRAL");
        assertThat(enviada.origem()).isSameAs(pedido.getRestaurante().getEndereco().getLocalizacao());
        assertThat(enviada.destino()).isSameAs(pedido.getEnderecoEntrega().getLocalizacao());
    }

    @Test
    void deveRegistrarPagamentoRejeitadoSemCriarEntrega() {
        pedido.confirmar();
        processador.responder(new ResultadoPagamento(StatusPagamento.REJEITADO, "TX-2", "recusado"));

        Pagamento pagamento = service.processar(1L, FormaPagamento.PIX, "TOKEN");

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGAMENTO_REJEITADO);
        assertThat(pagamento.getStatus()).isEqualTo(StatusPagamento.REJEITADO);
        verify(notificacaoService).notificarPagamento(pedido, StatusPagamento.REJEITADO);
        verify(entregaService, never()).criarPara(any());
    }

    @Test
    void naoDeveChamarProcessadorQuandoPedidoNaoEstaConfirmado() {
        assertThatThrownBy(() -> service.processar(1L, FormaPagamento.PIX, "TOKEN"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Pedido precisa estar confirmado para pagamento");
        assertThat(processador.ultimaSolicitacao).isNull();
    }

    @Test
    void deveRejeitarPedidoInexistente() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.processar(99L, FormaPagamento.PIX, "TOKEN"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThat(processador.ultimaSolicitacao).isNull();
    }

    private Endereco endereco(double latitude, double longitude) {
        return new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(latitude, longitude));
    }

    /** Implementação de teste do contrato: devolve uma resposta fixa e guarda o que recebeu. */
    private static class ProcessadorFalso implements ProcessadorPagamento {
        private ResultadoPagamento resposta;
        private SolicitacaoPagamento ultimaSolicitacao;

        void responder(ResultadoPagamento resposta) {
            this.resposta = resposta;
        }

        @Override
        public ResultadoPagamento processar(SolicitacaoPagamento solicitacao) {
            ultimaSolicitacao = solicitacao;
            return resposta;
        }
    }
}
