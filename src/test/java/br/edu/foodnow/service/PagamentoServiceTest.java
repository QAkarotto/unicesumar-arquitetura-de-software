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
import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagamentoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private GatewayPagamento gatewayPagamento;

    @Mock
    private NotificacaoService notificacaoService;

    @Mock
    private EntregaService entregaService;

    @Mock
    private LocalizacaoService localizacaoService;

    @Mock
    private LogisticaService logisticaService;

    @Mock
    private Pedido pedido;

    @Mock
    private Restaurante restaurante;

    @Mock
    private Endereco origem;

    @Mock
    private Endereco destino;

    private PagamentoService pagamentoService;

    @BeforeEach
    void configurarCenario() {
        pagamentoService = new PagamentoService(
                pedidoRepository,
                pagamentoRepository,
                gatewayPagamento,
                notificacaoService,
                entregaService,
                localizacaoService,
                logisticaService
        );

        when(pedidoRepository.findById(10L))
                .thenReturn(Optional.of(pedido));
        when(pedido.getStatus()).thenReturn(StatusPedido.CONFIRMADO);
        when(pedido.getId()).thenReturn(10L);
        when(pedido.getValorTotal()).thenReturn(new BigDecimal("42.30"));
        when(pedido.getRestaurante()).thenReturn(restaurante);
        when(pedido.getEnderecoEntrega()).thenReturn(destino);
        when(restaurante.getEndereco()).thenReturn(origem);
        when(origem.getLocalizacao())
                .thenReturn(new Localizacao(-25.100, -50.150));
        when(destino.getLocalizacao())
                .thenReturn(new Localizacao(-25.110, -50.160));

        when(localizacaoService.calcularRota(origem, destino))
                .thenReturn(new Rota(2.5, 10, "CENTRO"));

        when(logisticaService.resumirPedido(pedido))
                .thenReturn(new PoliticaEntregaAtual.ResumoPedido("CENTRO", 10));

        when(pagamentoRepository.save(any(Pagamento.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void deveProcessarPagamentoAprovadoUsandoAAbstracao() {
        when(gatewayPagamento.processar(any(SolicitacaoPagamento.class)))
                .thenReturn(new ResultadoPagamento(
                        "FN-10", "AUTHORIZED", "Pagamento autorizado"));

        Pagamento resultado = pagamentoService.processar(
                10L, FormaPagamento.PIX, "TOKEN-VALIDO");

        assertEquals(StatusPagamento.APROVADO, resultado.getStatus());
        assertEquals("FN-10", resultado.getCodigoExterno());

        verify(gatewayPagamento).processar(any(SolicitacaoPagamento.class));
        verify(pedido).registrarPagamento(StatusPagamento.APROVADO);
        verify(notificacaoService).notificarPagamento(
                pedido, StatusPagamento.APROVADO);
        verify(entregaService).criarPara(pedido);
    }

    @Test
    void deveRegistrarPagamentoRejeitadoSemCriarEntrega() {
        when(gatewayPagamento.processar(any(SolicitacaoPagamento.class)))
                .thenReturn(new ResultadoPagamento(
                        "FN-10", "DECLINED", "Pagamento rejeitado"));

        Pagamento resultado = pagamentoService.processar(
                10L, FormaPagamento.PIX, "REJEITADO");

        assertEquals(StatusPagamento.REJEITADO, resultado.getStatus());
        assertEquals("FN-10", resultado.getCodigoExterno());

        verify(gatewayPagamento).processar(any(SolicitacaoPagamento.class));
        verify(pedido).registrarPagamento(StatusPagamento.REJEITADO);
        verify(notificacaoService).notificarPagamento(
                pedido, StatusPagamento.REJEITADO);
        verify(entregaService, never()).criarPara(pedido);
    }
}
