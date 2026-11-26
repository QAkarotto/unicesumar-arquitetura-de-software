package br.edu.foodnow.service;

import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.PagamentoProcessado;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {
    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final GatewayPagamento gatewayPagamento;
    private final EntregaService entregaService;
    private final LocalizacaoService localizacaoService;
    private final LogisticaService logisticaService;
    private final ApplicationEventPublisher eventos;

    public PagamentoService(PedidoRepository pedidoRepository, PagamentoRepository pagamentoRepository,
                            GatewayPagamento gatewayPagamento, EntregaService entregaService,
                            LocalizacaoService localizacaoService, LogisticaService logisticaService,
                            ApplicationEventPublisher eventos) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.gatewayPagamento = gatewayPagamento;
        this.entregaService = entregaService;
        this.localizacaoService = localizacaoService;
        this.logisticaService = logisticaService;
        this.eventos = eventos;
    }

    @Transactional
    public Pagamento processar(Long pedidoId, FormaPagamento forma, String token) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new RegraNegocioException("Pedido precisa estar confirmado para pagamento");
        }

        var rotaPagamento = localizacaoService.calcularRota(pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega());
        String regiao = logisticaService.resumirPedido(pedido).regiao();
        ResultadoPagamento resultado = gatewayPagamento.autorizar(new SolicitacaoPagamento(
                pedido.getId(), pedido.getValorTotal(), token, forma, regiao,
                pedido.getRestaurante().getEndereco().getLocalizacao(),
                pedido.getEnderecoEntrega().getLocalizacao()));
        StatusPagamento status = resultado.status();

        pedido.registrarPagamento(status);
        pedidoRepository.save(pedido);
        Pagamento pagamento = pagamentoRepository.save(new Pagamento(pedido, forma, status,
                pedido.getValorTotal(), resultado.codigoTransacao(), resultado.mensagem(),
                regiao, rotaPagamento.distanciaKm()));
        if (status == StatusPagamento.APROVADO) {
            entregaService.criarPara(pedido);
        }
        eventos.publishEvent(new PagamentoProcessado(pagamento.getId(), pedido.getId(), status));
        return pagamento;
    }

    public Pagamento consultar(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado"));
    }
}
