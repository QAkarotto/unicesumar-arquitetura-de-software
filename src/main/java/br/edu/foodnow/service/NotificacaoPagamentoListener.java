package br.edu.foodnow.service;

import br.edu.foodnow.config.EventosConfig;
import br.edu.foodnow.pagamento.PagamentoProcessado;
import br.edu.foodnow.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumidor do evento {@link PagamentoProcessado}: envia o e-mail de pagamento ao cliente.
 * Executa após o commit do pagamento, em outra thread, e não propaga falhas para a operação principal.
 */
@Component
public class NotificacaoPagamentoListener {
    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoPagamentoListener.class);

    private final PedidoRepository pedidoRepository;
    private final NotificacaoService notificacaoService;

    public NotificacaoPagamentoListener(PedidoRepository pedidoRepository, NotificacaoService notificacaoService) {
        this.pedidoRepository = pedidoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Async(EventosConfig.EXECUTOR_EVENTOS)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void aoProcessarPagamento(PagamentoProcessado evento) {
        try {
            var pedido = pedidoRepository.findById(evento.pedidoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
            notificacaoService.notificarPagamento(pedido, evento.status());
        } catch (RuntimeException falha) {
            LOG.error("Falha ao notificar o pagamento {} do pedido {}; o pagamento permanece registrado",
                    evento.pagamentoId(), evento.pedidoId(), falha);
        }
    }
}
