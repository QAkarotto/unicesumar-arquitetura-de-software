package br.edu.foodnow.event;

import br.edu.foodnow.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PedidoConfirmadoConsumer {
    private static final Logger log = LoggerFactory.getLogger(PedidoConfirmadoConsumer.class);

    private final NotificacaoService notificacaoService;

    public PedidoConfirmadoConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Async("foodNowEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void consumir(PedidoConfirmadoEvent evento) {
        try {
            notificacaoService.notificarConfirmacao(evento);
        } catch (RuntimeException excecao) {
            // A confirmação já foi commitada; a falha secundária não deve alterar seu resultado.
            log.error("Falha ao enviar notificação do pedido confirmado {}", evento.pedidoId(), excecao);
        }
    }
}
