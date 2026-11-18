package br.edu.foodnow.event;

import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.service.NotificacaoService;

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

    private final PedidoRepository pedidoRepository;
    private final NotificacaoService notificacaoService;

    public PedidoConfirmadoListener(
            PedidoRepository pedidoRepository,
            NotificacaoService notificacaoService) {

        this.pedidoRepository = pedidoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Async("foodNowAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmarPedido(PedidoConfirmadoEvent evento) {

        try {
            Pedido pedido = pedidoRepository.findById(evento.pedidoId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Pedido confirmado não encontrado: " + evento.pedidoId()
                    ));

            notificacaoService.notificarConfirmacao(pedido);

        } catch (Exception excecao) {
            log.error(
                    "Falha ao processar notificação do pedido {}. "
                            + "O pedido permanece confirmado.",
                    evento.pedidoId(),
                    excecao
            );
        }
    }
}