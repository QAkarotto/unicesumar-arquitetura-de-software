package foodnow.service;

import foodnow.evento.PedidoConfirmado;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class PedidoService {

    private final ApplicationEventPublisher publisher;

    public PedidoService() {
        this(evento -> { });
    }

    @Autowired
    public PedidoService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void criarPedido(int pedidoId, int clienteId) {
        System.out.println("Pedido " + pedidoId + " criado para o cliente " + clienteId);
    }

    public void confirmarPedido(int pedidoId) {
        System.out.println("Pedido " + pedidoId + " confirmado.");
        publisher.publishEvent(new PedidoConfirmado(pedidoId, Instant.now()));
    }

    public void cancelarPedido(int pedidoId) {
        System.out.println("Pedido " + pedidoId + " cancelado.");
    }

    public double calcularValorTotal(double subtotal, double taxaEntrega) {
        return subtotal + taxaEntrega;
    }

    public String consultarStatusPedido(int pedidoId) {
        return "EM PREPARO";
    }
}
