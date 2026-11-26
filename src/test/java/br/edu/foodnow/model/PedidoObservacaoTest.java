package br.edu.foodnow.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoObservacaoTest {
    @Test
    void deveProtegerLimiteMesmoForaDaApi() {
        assertThatThrownBy(() -> new Pedido(null, null, null, "a".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Observação deve ter no máximo 500 caracteres");
    }
}
