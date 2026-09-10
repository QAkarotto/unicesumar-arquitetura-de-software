package br.edu.foodnow.service;

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
    private final LocalizacaoService localizacaoService;
    private final ValidadorEntrega validadorEntrega;

    public PagamentoService(PedidoRepository pedidoRepository, PagamentoRepository pagamentoRepository,
                            FakePaymentGateway paymentGateway, NotificacaoService notificacaoService,
                            EntregaService entregaService, LocalizacaoService localizacaoService,
                            ValidadorEntrega validadorEntrega) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.paymentGateway = paymentGateway;
        this.notificacaoService = notificacaoService;
        this.entregaService = entregaService;
        this.localizacaoService = localizacaoService;
        this.validadorEntrega = validadorEntrega;
    }

    @Transactional
    public Pagamento processar(Long pedidoId, FormaPagamento forma, String token) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new RegraNegocioException("Pedido precisa estar confirmado para pagamento");
        }

        double distanciaRota = localizacaoService.calcularDistancia(
                pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        String regiao = validadorEntrega.determinarRegiaoEntrega(pedido);
        FakePaymentGateway.GatewayRequest requisicao = FakePaymentGateway.GatewayRequest.from(
                pedido.getId(), pedido.getValorTotal(), token, forma.name(), regiao,
                pedido.getRestaurante().getEndereco().getLocalizacao().formatarParaProvedor(),
                pedido.getEnderecoEntrega().getLocalizacao().formatarParaProvedor());
        FakePaymentGateway.GatewayResult resposta = paymentGateway.processarPagamento(requisicao);
        StatusPagamento status = "AUTHORIZED".equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO : StatusPagamento.REJEITADO;

        pedido.registrarPagamento(status);
        pedidoRepository.save(pedido);
        Pagamento pagamento = pagamentoRepository.save(new Pagamento(pedido, forma, status,
                pedido.getValorTotal(), resposta.transactionCode(), resposta.providerMessage(),
                regiao, distanciaRota));
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