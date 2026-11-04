package br.edu.foodnow.evento;

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
public class ConsumidorEventoPagamento {

    private static final Logger logger =
            LoggerFactory.getLogger(ConsumidorEventoPagamento.class);

    private final PedidoRepository pedidoRepository;
    private final NotificacaoService notificacaoService;

    public ConsumidorEventoPagamento(
            PedidoRepository pedidoRepository,
            NotificacaoService notificacaoService) {
        this.pedidoRepository = pedidoRepository;
        this.notificacaoService = notificacaoService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processar(EventoPagamentoProcessamento evento) {
        try {
            Pedido pedido = pedidoRepository.findById(evento.pedidoId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Pedido não encontrado: " + evento.pedidoId()));

            notificacaoService.notificarPagamento(pedido, evento.status());

        } catch (RuntimeException erro) {
            logger.error(
                    "Falha ao enviar notificação do pagamento do pedido {}",
                    evento.pedidoId(),
                    erro);
        }
    }
}//*@Component: registra o consumidor no Spring.@Async: executa o método em outra thread. @TransactionalEventListener(AFTER_COMMIT): só recebe o evento depois que a transação principal for confirmada. */