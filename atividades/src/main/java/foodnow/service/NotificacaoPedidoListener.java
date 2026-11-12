package foodnow.service;

import foodnow.evento.PedidoConfirmado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoPedidoListener {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoPedidoListener.class);

    private final NotificacaoService notificacaoService;

    public NotificacaoPedidoListener(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Async
    @EventListener
    public void aoConfirmarPedido(PedidoConfirmado evento) {

        LOG.info(
                "Iniciando processamento da notificação do pedido {} em {}",
                evento.pedidoId(),
                evento.confirmadoEm()
        );

        try {

            notificacaoService.enviarConfirmacaoPedido(evento.pedidoId());

            LOG.info(
                    "Notificação do pedido {} enviada com sucesso",
                    evento.pedidoId()
            );

        } catch (RuntimeException e) {

            LOG.error(
                    "Falha ao enviar notificação do pedido {}. Motivo: {}",
                    evento.pedidoId(),
                    e.getMessage(),
                    e
            );
        }
    }
}