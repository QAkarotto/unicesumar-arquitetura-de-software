package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusPagamento;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {
    private final FakeEmailClient emailClient;

    public NotificacaoService(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }

    public void notificarPagamento(Pedido pedido, StatusPagamento status) {
        if (status == StatusPagamento.APROVADO) {
            emailClient.enviar(pedido.getCliente().getEmail(), "Pagamento aprovado",
                    "O pagamento do pedido " + pedido.getId() + " foi aprovado.");
        } else {
            emailClient.enviar(pedido.getCliente().getEmail(), "Pagamento rejeitado",
                    "O pagamento do pedido " + pedido.getId() + " foi rejeitado.");
        }
    }
}
