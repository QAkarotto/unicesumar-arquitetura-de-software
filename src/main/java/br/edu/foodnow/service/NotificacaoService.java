package br.edu.foodnow.service;

import br.edu.foodnow.entrega.PoliticaEntrega;
import br.edu.foodnow.geo.Rota;
import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {
    private final FakeEmailClient emailClient;
    private final PoliticaEntrega politicaEntrega;

    public NotificacaoService(FakeEmailClient emailClient, PoliticaEntrega politicaEntrega) {
        this.emailClient = emailClient;
        this.politicaEntrega = politicaEntrega;
    }

    public void notificarConfirmacao(Pedido pedido) {
        emailClient.enviar(pedido.getCliente().getEmail(), "Pedido confirmado",
                "O pedido " + pedido.getId() + " foi confirmado para a região "
                        + politicaEntrega.regiaoDeEntrega(pedido) + ". Estimativa interna: "
                        + politicaEntrega.estimarTempoInterno(pedido) + " minutos.");
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status, Rota rota) {
        if (status == StatusPagamento.APROVADO) {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento aprovado",
                    "O pagamento do pedido " + pedido.getId() + " foi aprovado. Rota estimada pelo e-mail: "
                            + rota.duracaoMinutos() + " minutos.")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            emailClient.enviar(notificacao);
        } else {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                            + rota.zona() + ".")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            emailClient.enviar(notificacao);
        }
    }

    public void notificarEntregaIniciada(Pedido pedido) {
        emailClient.enviar(pedido.getCliente().getEmail(), "Entrega iniciada",
                "O pedido " + pedido.getId() + " saiu para entrega.");
    }
}
