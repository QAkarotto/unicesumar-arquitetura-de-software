package br.edu.foodnow.service;

import br.edu.foodnow.entrega.AvaliacaoGeografica;
import br.edu.foodnow.entrega.PoliticaEntrega;
import br.edu.foodnow.integration.FakePaymentGateway;
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
    private final FakePaymentGateway paymentGateway;
    private final NotificacaoService notificacaoService;
    private final EntregaService entregaService;
    private final PoliticaEntrega politicaEntrega;

    public PagamentoService(PedidoRepository pedidoRepository, PagamentoRepository pagamentoRepository,
                            FakePaymentGateway paymentGateway, NotificacaoService notificacaoService,
                            EntregaService entregaService, PoliticaEntrega politicaEntrega) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.paymentGateway = paymentGateway;
        this.notificacaoService = notificacaoService;
        this.entregaService = entregaService;
        this.politicaEntrega = politicaEntrega;
    }

    @Transactional
    public Pagamento processar(Long pedidoId, FormaPagamento forma, String token) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new RegraNegocioException("Pedido precisa estar confirmado para pagamento");
        }

        AvaliacaoGeografica avaliacao = politicaEntrega.avaliar(pedido);
        FakePaymentGateway.GatewayRequest requisicao = FakePaymentGateway.GatewayRequest.from(
                pedido.getId(), pedido.getValorTotal(), token, forma.name(), avaliacao.regiao(),
                pedido.getRestaurante().getEndereco().getLocalizacao().formatarParaProvedor(),
                pedido.getEnderecoEntrega().getLocalizacao().formatarParaProvedor());
        FakePaymentGateway.GatewayResult resposta = paymentGateway.processarPagamento(requisicao);
        StatusPagamento status = "AUTHORIZED".equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;

        pedido.registrarPagamento(status);
        pedidoRepository.save(pedido);
        Pagamento pagamento = pagamentoRepository.save(new Pagamento(pedido, forma, status,
                pedido.getValorTotal(), resposta.transactionCode(), resposta.providerMessage(),
                avaliacao.regiao(), avaliacao.rota().distanciaKm()));
        notificacaoService.notificarPagamento(pedido, status, avaliacao.rota());
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
