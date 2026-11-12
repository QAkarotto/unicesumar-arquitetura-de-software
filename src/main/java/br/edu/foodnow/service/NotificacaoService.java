package br.edu.foodnow.service;

import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.notificacao.EmailGateway;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {

    private final EmailGateway emailGateway;
    private final LocalizacaoService localizacaoService;
    private final LogisticaService logisticaService;

    public NotificacaoService(
            EmailGateway emailGateway,
            LocalizacaoService localizacaoService,
            LogisticaService logisticaService) {

        this.emailGateway = emailGateway;
        this.localizacaoService = localizacaoService;
        this.logisticaService = logisticaService;
    }

    public void notificarConfirmacao(Pedido pedido) {
        var resumo = logisticaService.resumirPedido(pedido);

        emailGateway.enviar(
                pedido.getCliente().getEmail(),
                "Pedido confirmado",
                "O pedido " + pedido.getId()
                        + " foi confirmado para a região "
                        + resumo.regiao()
                        + ". Estimativa interna: "
                        + resumo.tempoMinutos() + " minutos."
        );
    }

    public void notificarInicioEntrega(Pedido pedido) {
        emailGateway.enviar(
                pedido.getCliente().getEmail(),
                "Entrega iniciada",
                "O pedido " + pedido.getId() + " saiu para entrega."
        );
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status) {
        var rota = localizacaoService.calcularRota(
                pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega()
        );

        if (status == StatusPagamento.APROVADO) {
            Notificacao notificacao = new Notificacao(
                    pedido.getCliente().getEmail(),
                    "Pagamento aprovado",
                    "O pagamento do pedido " + pedido.getId()
                            + " foi aprovado. Rota estimada pelo e-mail: "
                            + rota.tempoMinutos() + " minutos."
            ).adicionarReferenciaGeografica(pedido.getEnderecoEntrega());

            emailGateway.enviar(notificacao);

        } else {
            Notificacao notificacao = new Notificacao(
                    pedido.getCliente().getEmail(),
                    "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId()
                            + " foi rejeitado na zona "
                            + rota.zona() + "."
            ).adicionarReferenciaGeografica(pedido.getEnderecoEntrega());

            emailGateway.enviar(notificacao);
        }
    }
}
