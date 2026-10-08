package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import org.springframework.stereotype.Service;

/**
 * Ponto único de saída das notificações do FoodNow.
 *
 * <p>Antes o mesmo cliente de e-mail era alcançado por três caminhos independentes, direto do
 * pedido, direto da entrega e por aqui no pagamento, e este serviço ainda consultava o mapa por
 * conta própria só para escrever o texto. Agora todos os eventos passam por esta fachada e a rota
 * chega pronta de quem já a calculou, então adicionar SMS ou push é uma mudança contida aqui.</p>
 */
@Service
public class NotificacaoService {
    private final FakeEmailClient emailClient;

    public NotificacaoService(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public void notificarPedidoConfirmado(Pedido pedido) {
        emailClient.enviar(destinatario(pedido), "Pedido confirmado",
                "O pedido " + pedido.getId() + " foi confirmado para a região "
                        + pedido.determinarRegiaoEntrega() + ". Estimativa interna: "
                        + pedido.estimarTempoEntregaPeloPedido() + " minutos.");
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status, Rota rota) {
        Notificacao notificacao = status == StatusPagamento.APROVADO
                ? new Notificacao(destinatario(pedido), "Pagamento aprovado",
                        "O pagamento do pedido " + pedido.getId()
                                + " foi aprovado. Rota estimada pelo e-mail: "
                                + rota.duracaoMinutos() + " minutos.")
                : new Notificacao(destinatario(pedido), "Pagamento rejeitado",
                        "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                                + rota.regiao() + ".");
        emailClient.enviar(notificacao.adicionarReferenciaGeografica(pedido.getEnderecoEntrega()));
    }

    public void notificarEntregaIniciada(Pedido pedido) {
        emailClient.enviar(destinatario(pedido), "Entrega iniciada",
                "O pedido " + pedido.getId() + " saiu para entrega.");
    }

    private String destinatario(Pedido pedido) {
        return pedido.getCliente().getEmail();
    }
}
