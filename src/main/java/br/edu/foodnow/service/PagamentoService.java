
package br.edu.foodnow.service;

import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.pagamento.GatewayPagamento;
import br.edu.foodnow.pagamento.ResultadoPagamento;
import br.edu.foodnow.pagamento.SolicitacaoPagamento;
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
    private final LogisticaService logisticaService;

    public PagamentoService(
            PedidoRepository pedidoRepository,
            PagamentoRepository pagamentoRepository,
            GatewayPagamento paymentGateway,
            NotificacaoService notificacaoService,
            EntregaService entregaService,
            LocalizacaoService localizacaoService,
            LogisticaService logisticaService) {
        this.pedidoRepository = pedidoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.paymentGateway = paymentGateway;
        this.notificacaoService = notificacaoService;
        this.entregaService = entregaService;
        this.localizacaoService = localizacaoService;
        this.logisticaService = logisticaService;
    }

    @Transactional
    public Pagamento processar(Long pedidoId, FormaPagamento forma, String token) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido não encontrado"));

        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new RegraNegocioException(
                    "Pedido precisa estar confirmado para pagamento");
        }

        var rotaPagamento = localizacaoService.calcularRota(
                pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega());

        String regiao = logisticaService.resumirPedido(pedido).regiao();

        SolicitacaoPagamento requisicao = new SolicitacaoPagamento(
                pedido.getId().toString(),
                pedido.getValorTotal().movePointRight(2).longValueExact(),
                token,
                forma.name(),
                regiao,
                pedido.getRestaurante().getEndereco()
                        .getLocalizacao().formatarParaProvedor(),
                pedido.getEnderecoEntrega()
                        .getLocalizacao().formatarParaProvedor()
        );

        ResultadoPagamento resposta = paymentGateway.processar(requisicao);

        StatusPagamento status = "AUTHORIZED".equals(resposta.providerStatus())
                ? StatusPagamento.APROVADO
                : StatusPagamento.REJEITADO;

        pedido.registrarPagamento(status);
        pedidoRepository.save(pedido);

        Pagamento pagamento = pagamentoRepository.save(
                new Pagamento(
                        pedido,
                        forma,
                        status,
                        pedido.getValorTotal(),
                        resposta.transactionCode(),
                        resposta.providerMessage(),
                        regiao,
                        rotaPagamento.distanciaKm()
                )
        );

        notificacaoService.notificarPagamento(pedido, status);

        if (status == StatusPagamento.APROVADO) {
            entregaService.criarPara(pedido);
        }

        return pagamento;
    }

    public Pagamento consultar(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pagamento não encontrado"));
    }
}
