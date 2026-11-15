
package br.edu.foodnow.evento;

import br.edu.foodnow.model.Notificacao;
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

    private static final int MAX_TENTATIVAS = 3;
    private static final long ATRASO_ENTRE_TENTATIVAS_MS = 500;

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
        Pedido pedido;

        try {
            pedido = pedidoRepository.findById(evento.pedidoId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Pedido não encontrado: " + evento.pedidoId()));

        } catch (RuntimeException erro) {
            logger.error(
                    "Não foi possível localizar o pedido {} para notificação. Tipo de falha: {}",
                    evento.pedidoId(),
                    erro.getClass().getSimpleName(),
                    erro
            );
            return;
        }

        Notificacao notificacao;

        try {
            notificacao = notificacaoService.prepararNotificacao(
                    pedido, evento.status());

        } catch (RuntimeException erro) {
            logger.error(
                    "Não foi possível preparar a notificação do pedido {}. Tipo de falha: {}",
                    evento.pedidoId(),
                    erro.getClass().getSimpleName(),
                    erro
            );
            return;
        }

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                notificacaoService.enviarNotificacao(notificacao);

                logger.info(
                        "Notificação do pagamento do pedido {} enviada na tentativa {}/{}",
                        evento.pedidoId(),
                        tentativa,
                        MAX_TENTATIVAS
                );
                return;

            } catch (RuntimeException erro) {
                if (tentativa == MAX_TENTATIVAS) {
                    logger.error(
                            "Falha definitiva ao enviar notificação do pedido {} após {} tentativas. Tipo de falha: {}",
                            evento.pedidoId(),
                            MAX_TENTATIVAS,
                            erro.getClass().getSimpleName(),
                            erro
                    );
                    return;
                }

                logger.warn(
                        "Falha ao enviar notificação do pedido {} na tentativa {}/{}. Tipo de falha: {}. Nova tentativa em {} ms",
                        evento.pedidoId(),
                        tentativa,
                        MAX_TENTATIVAS,
                        erro.getClass().getSimpleName(),
                        ATRASO_ENTRE_TENTATIVAS_MS,
                        erro
                );

                try {
                    Thread.sleep(ATRASO_ENTRE_TENTATIVAS_MS);

                } catch (InterruptedException interrupcao) {
                    Thread.currentThread().interrupt();

                    logger.error(
                            "Tentativas de notificação do pedido {} interrompidas.",
                            evento.pedidoId(),
                            interrupcao
                    );
                    return;
                }
            }
        }
    }
}
