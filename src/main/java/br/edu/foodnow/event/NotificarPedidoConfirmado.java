package br.edu.foodnow.event;

import br.edu.foodnow.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificarPedidoConfirmado {
    private static final Logger log = LoggerFactory.getLogger(NotificarPedidoConfirmado.class);
    private final NotificacaoService notificacaoService;

    public NotificarPedidoConfirmado(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Async("eventosExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void consumir(PedidoConfirmado evento) {
        try {
            notificacaoService.notificarConfirmacao(evento.pedidoId(), evento.emailCliente(),
                    evento.regiaoEntrega(), evento.tempoEstimadoMinutos());
        } catch (Exception excecao) {
            log.error("Falha ao enviar notificação de confirmação do pedido {}", evento.pedidoId(), excecao);
        }
    }
}
