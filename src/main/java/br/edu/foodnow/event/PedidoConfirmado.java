package br.edu.foodnow.event;

public record PedidoConfirmado(Long pedidoId, String emailCliente, String regiaoEntrega,
                               int estimativaEntregaMinutos) {
}
