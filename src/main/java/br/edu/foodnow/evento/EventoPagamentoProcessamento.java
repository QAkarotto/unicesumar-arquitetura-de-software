package br.edu.foodnow.evento;

import br.edu.foodnow.model.StatusPagamento;

public record EventoPagamentoProcessamento(Long pedidoId, StatusPagamento status) {
}