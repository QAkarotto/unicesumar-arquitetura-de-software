package br.edu.foodnow.evento;

import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacaoPedidoConfirmadoListener {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoPedidoConfirmadoListener.class);

    private final PedidoRepository pedidoRepository;
    private final NotificacaoService notificacaoService;

    public NotificacaoPedidoConfirmadoListener(PedidoRepository pedidoRepository,
                                               NotificacaoService notificacaoService) {
        this.pedidoRepository = pedidoRepository;
        this.notificacaoService = notificacaoService;
    }

    // roda em outra thread e só depois do commit da confirmação
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void aoConfirmarPedido(PedidoConfirmado evento) {
        try {
            pedidoRepository.findById(evento.pedidoId()).ifPresentOrElse(
                    notificacaoService::notificarConfirmacao,
                    () -> log.warn("pedido {} não encontrado para notificar a confirmação", evento.pedidoId()));
        } catch (RuntimeException excecao) {
            // a confirmação já foi gravada, a falha fica só no log
            log.error("falha ao notificar a confirmação do pedido {}", evento.pedidoId(), excecao);
        }
    }
}
