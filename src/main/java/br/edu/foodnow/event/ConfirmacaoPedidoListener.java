package br.edu.foodnow.event;

import br.edu.foodnow.integration.FakeEmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ConfirmacaoPedidoListener {
    private static final Logger log = LoggerFactory.getLogger(ConfirmacaoPedidoListener.class);
    private final FakeEmailClient emailClient;

    public ConfirmacaoPedidoListener(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmar(PedidoConfirmado evento) {
        try {
            emailClient.enviar(evento.emailCliente(), "Pedido confirmado",
                    "O pedido " + evento.pedidoId() + " foi confirmado para a região "
                            + evento.regiaoEntrega() + ". Estimativa interna: "
                            + evento.estimativaEntregaMinutos() + " minutos.");
        } catch (RuntimeException falha) {
            log.warn("Falha ao enviar confirmação do pedido {}", evento.pedidoId(), falha);
        }
    }
}
