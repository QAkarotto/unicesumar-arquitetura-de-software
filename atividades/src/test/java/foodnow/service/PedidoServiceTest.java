package foodnow.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import foodnow.evento.PedidoConfirmado;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class PedidoServiceTest {

    @Test
    void devePublicarEventoAoConfirmarPedido() {
        List<Object> publicados = new ArrayList<>();
        PedidoService pedidoService = new PedidoService(publicados::add);

        pedidoService.confirmarPedido(10);

        assertEquals(1, publicados.size());
        PedidoConfirmado evento = assertInstanceOf(PedidoConfirmado.class, publicados.get(0));
        assertEquals(10, evento.pedidoId());
    }

    @Test
    void naoDevePublicarEventoEmOutrasOperacoes() {
        List<Object> publicados = new ArrayList<>();
        PedidoService pedidoService = new PedidoService(publicados::add);

        pedidoService.criarPedido(1, 2);
        pedidoService.cancelarPedido(1);

        assertEquals(0, publicados.size());
    }

    @Test
    void construtorSemArgumentosContinuaFuncionando() {
        PedidoService pedidoService = new PedidoService();

        assertDoesNotThrow(() -> pedidoService.confirmarPedido(1));
        assertEquals("EM PREPARO", pedidoService.consultarStatusPedido(1));
    }
}
