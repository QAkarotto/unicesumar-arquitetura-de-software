
package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {

    private final FakeEmailClient emailClient;
    private final LocalizacaoService localizacaoService;

    public NotificacaoService(
            FakeEmailClient emailClient,
            LocalizacaoService localizacaoService) {
        this.emailClient = emailClient;
        this.localizacaoService = localizacaoService;
    }

    public Notificacao prepararNotificacao(
            Pedido pedido,
            StatusPagamento status) {

        int tempo = localizacaoService.estimarTempoEntrega(
                pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega());

        String zona = localizacaoService.buscarZonaEntrega(
                pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega());

        Notificacao notificacao;

        if (status == StatusPagamento.APROVADO) {
            notificacao = new Notificacao(
                    pedido.getCliente().getEmail(),
                    "Pagamento aprovado",
                    "O pagamento do pedido " + pedido.getId()
                            + " foi aprovado. Rota estimada pelo e-mail: "
                            + tempo + " minutos.");

        } else {
            notificacao = new Notificacao(
                    pedido.getCliente().getEmail(),
                    "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId()
                            + " foi rejeitado na zona " + zona + ".");
        }

        return notificacao.adicionarReferenciaGeografica(
                pedido.getEnderecoEntrega());
    }

    public void enviarNotificacao(Notificacao notificacao) {
        emailClient.enviar(notificacao);
    }

    // Mantém o método anterior para preservar compatibilidade.
    public void notificarPagamento(
            Pedido pedido,
            StatusPagamento status) {

        Notificacao notificacao = prepararNotificacao(pedido, status);
        enviarNotificacao(notificacao);
    }
}
