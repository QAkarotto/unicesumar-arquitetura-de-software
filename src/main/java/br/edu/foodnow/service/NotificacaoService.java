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

    public NotificacaoService(FakeEmailClient emailClient, LocalizacaoService localizacaoService) {
        this.emailClient = emailClient;
        this.localizacaoService = localizacaoService;
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status) {
        LocalizacaoService.RotaEntrega rota = localizacaoService.avaliarRota(
                pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        if (status == StatusPagamento.APROVADO) {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento aprovado",
                    "O pagamento do pedido " + pedido.getId() + " foi aprovado. Rota estimada pelo e-mail: "
                            + rota.tempoEstimadoMinutos() + " minutos.")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            emailClient.enviar(notificacao);
        } else {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                            + rota.zonaEntrega() + ".")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            emailClient.enviar(notificacao);
        }
    }
}
