package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.logistica.LogisticaService;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.notificacao.CanalNotificacao;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {

    private final CanalNotificacao canalNotificacao;
    private final LocalizacaoService localizacaoService;
    private final LogisticaService logisticaService;

    public NotificacaoService(CanalNotificacao canalNotificacao,
                              LocalizacaoService localizacaoService,
                              LogisticaService logisticaService) {
        this.canalNotificacao = canalNotificacao;
        this.localizacaoService = localizacaoService;
        this.logisticaService = logisticaService;
    }

    public void notificarConfirmacao(Pedido pedido) {
        var resumo = logisticaService.resumirPedido(pedido);

        Notificacao notificacao = new Notificacao(
                pedido.getCliente().getEmail(),
                "Pedido confirmado",
                "O pedido " + pedido.getId() + " foi confirmado para a região "
                        + resumo.regiao() + ". Estimativa interna: "
                        + resumo.tempoMinutos() + " minutos."
        );

        canalNotificacao.enviar(notificacao);
    }

    public void notificarInicioEntrega(Pedido pedido) {
        Notificacao notificacao = new Notificacao(
                pedido.getCliente().getEmail(),
                "Entrega iniciada",
                "O pedido " + pedido.getId() + " saiu para entrega."
        );

        canalNotificacao.enviar(notificacao);
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status) {
        Rota rota = localizacaoService.calcularRota(
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

            canalNotificacao.enviar(notificacao);

        } else {
            Notificacao notificacao = new Notificacao(
                    pedido.getCliente().getEmail(),
                    "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId()
                            + " foi rejeitado na zona " + rota.zona() + "."
            ).adicionarReferenciaGeografica(pedido.getEnderecoEntrega());

            canalNotificacao.enviar(notificacao);
        }
    }
}