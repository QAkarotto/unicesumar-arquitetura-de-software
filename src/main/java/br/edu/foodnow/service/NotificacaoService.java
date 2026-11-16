package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {
    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

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
        executarSemInterromperFluxo("notificarConfirmacao", pedido, () -> {
            var resumo = logisticaService.resumirPedido(pedido);
            emailClient.enviar(pedido.getCliente().getEmail(), "Pedido confirmado",
                    "O pedido " + pedido.getId() + " foi confirmado para a região "
                            + resumo.regiao() + ". Estimativa interna: " + resumo.tempoMinutos() + " minutos.");
        });
    }

    public void notificarInicioEntrega(Pedido pedido) {
        executarSemInterromperFluxo("notificarInicioEntrega", pedido, () ->
                emailClient.enviar(pedido.getCliente().getEmail(), "Entrega iniciada",
                        "O pedido " + pedido.getId() + " saiu para entrega."));
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status) {
        executarSemInterromperFluxo("notificarPagamento", pedido, () -> {
            var rota = localizacaoService.calcularRota(pedido.getRestaurante().getEndereco(),
                    pedido.getEnderecoEntrega());
            if (status == StatusPagamento.APROVADO) {
                Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento aprovado",
                        "O pagamento do pedido " + pedido.getId() + " foi aprovado. Rota estimada pelo e-mail: "
                                + rota.tempoMinutos() + " minutos.")
                        .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
                emailClient.enviar(notificacao);
            } else {
                Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento rejeitado",
                        "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                                + rota.zona() + ".")
                        .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
                emailClient.enviar(notificacao);
            }
        });
    }

    /**
     * A notificação é um efeito colateral do fluxo principal (confirmação, pagamento, entrega).
     * Uma falha no envio não deve desfazer a transação do chamador nem deixar um pagamento já
     * autorizado no provedor sem registro local. Por isso a falha é tratada aqui e registrada
     * no log, sem repetir o envio, para não duplicar e-mails caso a primeira tentativa tenha
     * chegado ao destino. O log traz só a operação, o pedido e o tipo da falha: nunca o
     * destinatário, o conteúdo da mensagem nem coordenadas.
     */
    private void executarSemInterromperFluxo(String operacao, Pedido pedido, Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException falha) {
            log.error("Falha ao enviar notificação; fluxo principal preservado: operacao={}, pedidoId={}, tipo={}",
                    operacao, pedido.getId(), falha.getClass().getSimpleName());
        }
    }
}
