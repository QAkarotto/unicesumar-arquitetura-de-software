package br.edu.foodnow.service;

import br.edu.foodnow.config.EventosConfig;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.pagamento.PagamentoProcessado;
import br.edu.foodnow.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumidor do evento {@link PagamentoProcessado}: envia o e-mail de pagamento ao cliente.
 * Executa após o commit do pagamento, em outra thread, e não propaga falhas para a operação principal.
 * <p>
 * Resiliência: repete o envio em falhas, com limite de tentativas e espera crescente, e notifica cada
 * pagamento no máximo uma vez, ignorando eventos duplicados. Os registros de diagnóstico identificam a
 * operação, o pagamento, a tentativa, o tipo de falha e o resultado, sem dados pessoais nem tokens.
 */
@Component
public class NotificacaoPagamentoListener {
    static final String OPERACAO = "notificacao-pagamento";
    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoPagamentoListener.class);

    private final PedidoRepository pedidoRepository;
    private final NotificacaoService notificacaoService;
    private final int maximoTentativas;
    private final Duration esperaInicial;
    private final Set<Long> pagamentosNotificados = ConcurrentHashMap.newKeySet();

    public NotificacaoPagamentoListener(PedidoRepository pedidoRepository, NotificacaoService notificacaoService) {
        this(pedidoRepository, notificacaoService, 3, Duration.ofMillis(200));
    }

    @Autowired
    public NotificacaoPagamentoListener(PedidoRepository pedidoRepository, NotificacaoService notificacaoService,
            @Value("${foodnow.notificacao.pagamento.tentativas:3}") int maximoTentativas,
            @Value("${foodnow.notificacao.pagamento.espera-inicial:200ms}") Duration esperaInicial) {
        if (maximoTentativas < 1) {
            throw new IllegalArgumentException("Deve haver ao menos uma tentativa");
        }
        this.pedidoRepository = pedidoRepository;
        this.notificacaoService = notificacaoService;
        this.maximoTentativas = maximoTentativas;
        this.esperaInicial = esperaInicial;
    }

    @Async(EventosConfig.EXECUTOR_EVENTOS)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void aoProcessarPagamento(PagamentoProcessado evento) {
        if (evento.pagamentoId() != null && !pagamentosNotificados.add(evento.pagamentoId())) {
            LOG.warn("operacao={} pagamentoId={} pedidoId={} resultado=DUPLICADO_IGNORADO",
                    OPERACAO, evento.pagamentoId(), evento.pedidoId());
            return;
        }
        Optional<Pedido> pedido = pedidoRepository.findById(evento.pedidoId());
        if (pedido.isEmpty()) {
            LOG.error("operacao={} pagamentoId={} pedidoId={} falha=PEDIDO_NAO_ENCONTRADO resultado=FALHA_DEFINITIVA",
                    OPERACAO, evento.pagamentoId(), evento.pedidoId());
            liberarParaReprocessamento(evento);
            return;
        }
        enviarComRetry(evento, pedido.get());
    }

    private void enviarComRetry(PagamentoProcessado evento, Pedido pedido) {
        for (int tentativa = 1; tentativa <= maximoTentativas; tentativa++) {
            try {
                notificacaoService.notificarPagamento(pedido, evento.status());
                LOG.info("operacao={} pagamentoId={} pedidoId={} statusPagamento={} tentativas={} resultado=ENVIADO",
                        OPERACAO, evento.pagamentoId(), evento.pedidoId(), evento.status(), tentativa);
                return;
            } catch (RuntimeException falha) {
                String tipoFalha = falha.getClass().getSimpleName();
                if (tentativa == maximoTentativas) {
                    LOG.error("operacao={} pagamentoId={} pedidoId={} tentativas={} falha={} resultado=FALHA_DEFINITIVA",
                            OPERACAO, evento.pagamentoId(), evento.pedidoId(), tentativa, tipoFalha);
                    liberarParaReprocessamento(evento);
                    return;
                }
                Duration espera = esperaInicial.multipliedBy(1L << (tentativa - 1));
                LOG.warn("operacao={} pagamentoId={} pedidoId={} tentativa={}/{} falha={} acao=NOVA_TENTATIVA esperaMs={}",
                        OPERACAO, evento.pagamentoId(), evento.pedidoId(), tentativa, maximoTentativas, tipoFalha,
                        espera.toMillis());
                if (!aguardar(espera)) {
                    LOG.error("operacao={} pagamentoId={} pedidoId={} tentativas={} falha=INTERROMPIDO resultado=FALHA_DEFINITIVA",
                            OPERACAO, evento.pagamentoId(), evento.pedidoId(), tentativa);
                    liberarParaReprocessamento(evento);
                    return;
                }
            }
        }
    }

    private void liberarParaReprocessamento(PagamentoProcessado evento) {
        if (evento.pagamentoId() != null) {
            pagamentosNotificados.remove(evento.pagamentoId());
        }
    }

    private boolean aguardar(Duration espera) {
        try {
            Thread.sleep(espera.toMillis());
            return true;
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
