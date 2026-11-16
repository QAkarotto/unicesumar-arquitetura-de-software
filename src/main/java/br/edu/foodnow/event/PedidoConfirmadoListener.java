package br.edu.foodnow.event;

import br.edu.foodnow.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PedidoConfirmadoListener {
    private static final Logger logger = LoggerFactory.getLogger(PedidoConfirmadoListener.class);
    private final NotificacaoService notificacaoService;

    public PedidoConfirmadoListener(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Async("notificacoesExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void consumir(PedidoConfirmado evento) {
        try {
            notificacaoService.notificarConfirmacao(evento);
        } catch (RuntimeException excecao) {
            logger.error("Falha na notificação do pedido {}; confirmação preservada. Tipo: {}",
                    evento.pedidoId(), excecao.getClass().getSimpleName());
        }
    }
}
