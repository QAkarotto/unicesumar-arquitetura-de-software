package foodnow.evento;

import java.time.Instant;

public record PedidoConfirmado(int pedidoId, Instant confirmadoEm) {
}
