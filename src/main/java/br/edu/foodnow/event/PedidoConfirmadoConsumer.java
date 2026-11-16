package br.edu.foodnow.event;

import br.edu.foodnow.service.NotificacaoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PedidoConfirmadoConsumer {
    private static final Logger log = LoggerFactory.getLogger(PedidoConfirmadoConsumer.class);

    private final NotificacaoService notificacaoService;
    private final Set<Long> pedidosProcessados = ConcurrentHashMap.newKeySet();

    public PedidoConfirmadoConsumer(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Async("foodNowEventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void consumir(PedidoConfirmadoEvent evento) {
        Long pedidoId = evento.pedidoId();

        // A inclusão é atômica: somente uma execução assume o processamento do pedido.
        if (!pedidosProcessados.add(pedidoId)) {
            log.warn("Evento duplicado ignorado na notificacao de confirmacao. pedidoId={}", pedidoId);
            return;
        }

        log.info("Iniciando notificacao de confirmacao. pedidoId={}", pedidoId);
        try {
            notificacaoService.notificarConfirmacao(evento);
            log.info("Notificacao de confirmacao enviada. pedidoId={}", pedidoId);
        } catch (RuntimeException excecao) {
            // A confirmação principal já foi commitada. Libera a chave para uma nova entrega do evento.
            pedidosProcessados.remove(pedidoId);
            log.error(
                    "Falha na notificacao de confirmacao. pedidoId={}, tipoFalha={}, resultado=CHAVE_LIBERADA_PARA_NOVA_ENTREGA",
                    pedidoId, excecao.getClass().getSimpleName());
        }
    }
}
