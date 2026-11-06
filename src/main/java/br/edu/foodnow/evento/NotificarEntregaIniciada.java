package br.edu.foodnow.evento;

import br.edu.foodnow.integration.FakeEmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificarEntregaIniciada {
    private static final Logger log = LoggerFactory.getLogger(NotificarEntregaIniciada.class);
    private final FakeEmailClient emailClient;

    public NotificarEntregaIniciada(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Async("eventosExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void receber(EntregaIniciada evento) {
        try {
            emailClient.enviar(evento.emailCliente(), "Entrega iniciada",
                    "O pedido " + evento.pedidoId() + " saiu para entrega.");
        } catch (RuntimeException falha) {
            log.error("Falha ao notificar o início da entrega {} do pedido {}.",
                    evento.entregaId(), evento.pedidoId(), falha);
        }
    }
}
