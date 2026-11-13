package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.logistica.PoliticaEntregaAtual;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
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
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PagamentoServiceTest {
    private final PedidoRepository pedidoRepository = mock(PedidoRepository.class);
    private final PagamentoRepository pagamentoRepository = mock(PagamentoRepository.class);
    private final ProcessadorPagamento processadorPagamento = mock(ProcessadorPagamento.class);
    private final NotificacaoService notificacaoService = mock(NotificacaoService.class);
    private final EntregaService entregaService = mock(EntregaService.class);
    private final LocalizacaoService localizacaoService = mock(LocalizacaoService.class);
    private final LogisticaService logisticaService = mock(LogisticaService.class);
    private final Pedido pedido = mock(Pedido.class);
    private final PagamentoService service = new PagamentoService(pedidoRepository,
            pagamentoRepository, processadorPagamento, notificacaoService,
            entregaService, localizacaoService, logisticaService);

    @BeforeEach
    void prepararPedido() {
        var origem = mock(Endereco.class);
        var destino = mock(Endereco.class);
        var restaurante = mock(Restaurante.class);
        when(pedidoRepository.findById(10L)).thenReturn(Optional.of(pedido));
        when(pedido.getId()).thenReturn(10L);
        when(pedido.getStatus()).thenReturn(StatusPedido.CONFIRMADO);
        when(pedido.getValorTotal()).thenReturn(new BigDecimal("42.30"));
        when(pedido.getRestaurante()).thenReturn(restaurante);
        when(restaurante.getEndereco()).thenReturn(origem);
        when(pedido.getEnderecoEntrega()).thenReturn(destino);
        when(origem.getLocalizacao()).thenReturn(new Localizacao(-25.0, -50.0));
        when(destino.getLocalizacao()).thenReturn(new Localizacao(-25.1, -50.1));
        when(localizacaoService.calcularRota(origem, destino))
                .thenReturn(new Rota(8.5, 25, "CENTRAL"));
        when(logisticaService.resumirPedido(pedido))
                .thenReturn(new PoliticaEntregaAtual.ResumoPedido("CENTRAL", 25));
        when(pagamentoRepository.save(any(Pagamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveProcessarRegraSemDependerDoGatewayConcreto() {
        when(processadorPagamento.processar(any())).thenReturn(
                new ResultadoPagamento(StatusPagamento.APROVADO, "TX-10", "Autorizado"));

        Pagamento pagamento = service.processar(10L, FormaPagamento.CARTAO_CREDITO, "TOKEN");

        var captor = ArgumentCaptor.forClass(SolicitacaoPagamento.class);
        verify(processadorPagamento).processar(captor.capture());
        assertEquals(10L, captor.getValue().pedidoId());
        assertEquals(new BigDecimal("42.30"), captor.getValue().valor());
        assertEquals("CENTRAL", captor.getValue().regiao());
        assertEquals(StatusPagamento.APROVADO, pagamento.getStatus());
        verify(pedido).registrarPagamento(StatusPagamento.APROVADO);
        verify(entregaService).criarPara(pedido);
    }

    @Test
    void naoDeveCriarEntregaQuandoContratoRetornarPagamentoRejeitado() {
        when(processadorPagamento.processar(any())).thenReturn(
                new ResultadoPagamento(StatusPagamento.REJEITADO, "TX-10", "Recusado"));

        Pagamento pagamento = service.processar(10L, FormaPagamento.PIX, "TOKEN");

        assertEquals(StatusPagamento.REJEITADO, pagamento.getStatus());
        verify(pedido).registrarPagamento(StatusPagamento.REJEITADO);
        verify(entregaService, never()).criarPara(any());
    }
}
