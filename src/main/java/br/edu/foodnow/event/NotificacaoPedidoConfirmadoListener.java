package br.edu.foodnow.event;

import br.edu.foodnow.integration.FakeEmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificacaoPedidoConfirmadoListener {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoPedidoConfirmadoListener.class);
    private final FakeEmailClient emailClient;

    public NotificacaoPedidoConfirmadoListener(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmarPedido(PedidoConfirmado evento) {
        try {
            emailClient.enviar(evento.emailCliente(), "Pedido confirmado",
                    "O pedido " + evento.pedidoId() + " foi confirmado para a região "
                            + evento.regiao() + ". Estimativa interna: "
                            + evento.estimativaMinutos() + " minutos.");
        } catch (RuntimeException falha) {
            log.error("Falha ao notificar confirmação do pedido {}", evento.pedidoId(), falha);
        }
    }
}
