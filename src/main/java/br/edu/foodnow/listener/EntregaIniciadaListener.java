package br.edu.foodnow.listener;

import br.edu.foodnow.event.EntregaIniciadaEvent;
import br.edu.foodnow.integration.FakeEmailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EntregaIniciadaListener {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(EntregaIniciadaListener.class);

    private final FakeEmailClient emailClient;

    public EntregaIniciadaListener(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoIniciarEntrega(EntregaIniciadaEvent evento) {

        try {
            emailClient.enviar(
                    evento.destinatario(),
                    "Entrega iniciada",
                    "O pedido " + evento.pedidoId()
                            + " saiu para entrega."
            );

            LOGGER.info(
                    "Notificação da entrega {} enviada com sucesso.",
                    evento.entregaId()
            );

        } catch (RuntimeException erro) {

            LOGGER.error(
                    "Falha ao enviar notificação da entrega {} "
                            + "do pedido {}. A operação principal "
                            + "já foi confirmada.",
                    evento.entregaId(),
                    evento.pedidoId(),
                    erro
            );
        }
    }
}