package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.integration.EmailIndisponivelException;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificacaoService {
    private static final Logger logger = LoggerFactory.getLogger(NotificacaoService.class);
    private final FakeEmailClient emailClient;
    private final LocalizacaoService localizacaoService;
    private final LogisticaService logisticaService;

    public NotificacaoService(FakeEmailClient emailClient, LocalizacaoService localizacaoService,
                              LogisticaService logisticaService) {
        this.emailClient = emailClient;
        this.localizacaoService = localizacaoService;
        this.logisticaService = logisticaService;
    }

    public void notificarConfirmacao(Pedido pedido) {
        var resumo = logisticaService.resumirPedido(pedido);
        emailClient.enviar(pedido.getCliente().getEmail(), "Pedido confirmado",
                "O pedido " + pedido.getId() + " foi confirmado para a região "
                        + resumo.regiao() + ". Estimativa interna: " + resumo.tempoMinutos() + " minutos.");
    }

    public void notificarInicioEntrega(Pedido pedido) {
        emailClient.enviar(pedido.getCliente().getEmail(), "Entrega iniciada",
                "O pedido " + pedido.getId() + " saiu para entrega.");
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status) {
        var rota = localizacaoService.calcularRota(pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        if (status == StatusPagamento.APROVADO) {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento aprovado",
                    "O pagamento do pedido " + pedido.getId() + " foi aprovado. Rota estimada pelo e-mail: "
                            + rota.tempoMinutos() + " minutos.")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            enviarNotificacaoPagamento(pedido, status, notificacao);
        } else {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                            + rota.zona() + ".")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            enviarNotificacaoPagamento(pedido, status, notificacao);
        }
    }

    private void enviarNotificacaoPagamento(Pedido pedido, StatusPagamento status, Notificacao notificacao) {
        try {
            emailClient.enviar(notificacao);
            logger.info("operacao=notificarPagamento pedidoId={} status={} resultado=ENVIADA tentativas=1",
                    pedido.getId(), status);
        } catch (EmailIndisponivelException falha) {
            logger.warn("operacao=notificarPagamento pedidoId={} status={} tipoFalha={} "
                            + "resultado=ENVIO_INCERTO tratamento=CONTINUAR_SEM_REENVIO tentativas=1",
                    pedido.getId(), status, falha.getClass().getSimpleName());
        }
    }

}
