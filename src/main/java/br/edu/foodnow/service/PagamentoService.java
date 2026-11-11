
package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakePaymentGateway;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.model.StatusPedido;
import br.edu.foodnow.repository.PagamentoRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagamentoService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PagamentoService.class);

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final FakePaymentGateway paymentGateway;
    private final NotificacaoService notificacaoService;
    private final EntregaService entregaService;
    private final LocalizacaoService localizacaoService;
    private final LogisticaService logisticaService;

    public PagamentoService(
            PedidoRepository pedidoRepository,
            PagamentoRepository pagamentoRepository,
            FakePaymentGateway paymentGateway,
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
    public Pagamento processar(
            Long pedidoId,
            FormaPagamento forma,
            String token) {

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Pedido não encontrado"
                        ));

        if (pedido.getStatus() != StatusPedido.CONFIRMADO) {
            throw new RegraNegocioException(
                    "Pedido precisa estar confirmado para pagamento"
            );
        }

        var rotaPagamento = localizacaoService.calcularRota(
                pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega()
        );

        String regiao = logisticaService.resumirPedido(pedido).regiao();

        FakePaymentGateway.GatewayRequest requisicao =
                FakePaymentGateway.GatewayRequest.from(
                        pedido.getId(),
                        pedido.getValorTotal(),
                        token,
                        forma.name(),
                        regiao,
                        pedido.getRestaurante().getEndereco()
                                .getLocalizacao().formatarParaProvedor(),
                        pedido.getEnderecoEntrega()
                                .getLocalizacao().formatarParaProvedor()
                );

        FakePaymentGateway.GatewayResult resposta =
                paymentGateway.processarPagamento(requisicao);

        StatusPagamento status =
                "AUTHORIZED".equals(resposta.providerStatus())
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

        notificarSemComprometerPagamento(pedido, pagamento);

        if (status == StatusPagamento.APROVADO) {
            entregaService.criarPara(pedido);
        }

        return pagamento;
    }

    private void notificarSemComprometerPagamento(
            Pedido pedido,
            Pagamento pagamento) {

        try {
            notificacaoService.notificarPagamento(
                    pedido,
                    pagamento.getStatus()
            );

            LOGGER.info(
                    "Notificacao de pagamento processada. "
                            + "pedidoId={} pagamentoId={} status={} resultado=ENVIADA",
                    pedido.getId(),
                    pagamento.getId(),
                    pagamento.getStatus()
            );

        } catch (RuntimeException erro) {

            LOGGER.error(
                    "Falha no envio da notificacao de pagamento. "
                            + "pedidoId={} pagamentoId={} status={} "
                            + "tipoFalha={} resultado=NAO_ENVIADA; "
                            + "pagamento preservado",
                    pedido.getId(),
                    pagamento.getId(),
                    pagamento.getStatus(),
                    erro.getClass().getSimpleName()
            );
        }
    }

    public Pagamento consultar(Long id) {
        return pagamentoRepository.findById(id)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException(
                                "Pagamento não encontrado"
                        ));
    }
}
