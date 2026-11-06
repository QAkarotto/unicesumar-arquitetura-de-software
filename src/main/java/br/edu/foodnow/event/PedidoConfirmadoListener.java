
package br.edu.foodnow.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PedidoConfirmadoListener {

    private static final Logger log =
            LoggerFactory.getLogger(PedidoConfirmadoListener.class);

    private final RegistroConfirmacaoLog registro;

    public PedidoConfirmadoListener(RegistroConfirmacaoLog registro) {
        this.registro = registro;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processar(PedidoConfirmadoEvent evento) {
        try {
            registro.registrar(evento.pedidoId());
        } catch (Exception erro) {
            log.error(
                "Falha ao registrar confirmacao do pedido {}",
                evento.pedidoId(),
                erro
            );
        }
    }
}
