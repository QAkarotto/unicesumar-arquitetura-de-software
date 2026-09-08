package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Notificacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {
    private final FakeEmailClient emailClient;
    private final FakeMapsClient mapsClient;

    public NotificacaoService(FakeEmailClient emailClient, FakeMapsClient mapsClient) {
        this.emailClient = emailClient;
        this.mapsClient = mapsClient;
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status) {
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(
                pedido.getRestaurante().getEndereco().getLocalizacao(),
                pedido.getEnderecoEntrega().getLocalizacao());
        if (status == StatusPagamento.APROVADO) {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento aprovado",
                    "O pagamento do pedido " + pedido.getId() + " foi aprovado. Rota estimada pelo e-mail: "
                            + rota.durationMinutes() + " minutos.")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            emailClient.enviar(notificacao);
        } else {
            Notificacao notificacao = new Notificacao(pedido.getCliente().getEmail(), "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                            + rota.deliveryZone() + ".")
                    .adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
            emailClient.enviar(notificacao);
        }
    }
}
