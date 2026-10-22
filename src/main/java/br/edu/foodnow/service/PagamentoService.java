package br.edu.foodnow.service;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {
    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final GatewayPagamento paymentGateway;
    private final NotificacaoService notificacaoService;
    private final EntregaService entregaService;
    private final LocalizacaoService localizacaoService;

    public PagamentoService(PedidoRepository pedidoRepository, PagamentoRepository pagamentoRepository,
                            GatewayPagamento paymentGateway, NotificacaoService notificacaoService,
                            EntregaService entregaService, LocalizacaoService localizacaoService) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.paymentGateway = paymentGateway;
        this.notificacaoService = notificacaoService;
        this.entregaService = entregaService;
        this.localizacaoService = localizacaoService;
    }

    @Transactional
    public Pagamento processar(Long pedidoId, FormaPagamento forma, String token) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new RegraNegocioException("Pedido precisa estar confirmado para pagamento");
        }

        LocalizacaoService.RotaEntrega rotaPagamento = localizacaoService.avaliarRota(
                pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        String regiao = pedido.determinarRegiaoEntrega();
        GatewayPagamento.RequisicaoPagamento requisicao = new GatewayPagamento.RequisicaoPagamento(
                pedido.getId(), pedido.getValorTotal(), token, forma.name(), regiao,
                rotaPagamento.origemFormatada(), rotaPagamento.destinoFormatada());
        GatewayPagamento.ResultadoPagamento resposta = paymentGateway.processarPagamento(requisicao);
        StatusPagamento status = resposta.aprovado()
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;

        pedido.registrarPagamento(status);
        pedidoRepository.save(pedido);
        Pagamento pagamento = pagamentoRepository.save(new Pagamento(pedido, forma, status,
                pedido.getValorTotal(), resposta.codigoTransacao(), resposta.mensagem(),
                regiao, rotaPagamento.distanciaKm()));
        notificacaoService.notificarPagamento(pedido, status);
        if (status == StatusPagamento.APROVADO) {
            entregaService.criarPara(pedido);
        }
        return pagamento;
    }

    public Pagamento consultar(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento não encontrado"));
    }
}
