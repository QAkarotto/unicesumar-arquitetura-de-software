package br.edu.foodnow.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PedidoTest {

    @Test
    void deveCalcularSubtotalTaxaETotal() {
        Cenario cenario = cenarioPadrao();
        cenario.pedido().adicionarItem(cenario.produto(), 2);
        cenario.pedido().definirEntrega(3.2, new BigDecimal("8.34"));

        assertThat(cenario.pedido().getSubtotal()).isEqualByComparingTo("50.00");
        assertThat(cenario.pedido().getTaxaEntrega()).isEqualByComparingTo("8.34");
        assertThat(cenario.pedido().getValorTotal()).isEqualByComparingTo("58.34");
        assertThat(cenario.pedido().getItens()).hasSize(1);
        assertThat(cenario.produto().podeSerEntregueEm(cenario.endereco())).isTrue();
        assertThat(cenario.produto().calcularAdicionalRegional(cenario.endereco())).isZero();
        assertThat(cenario.pedido().getItens().getFirst().calcularParcelaGeografica(cenario.endereco())).isZero();
    }

    @Test
    void deveRejeitarQuantidadeInvalida() {
        Cenario cenario = cenarioPadrao();
        assertThatThrownBy(() -> cenario.pedido().adicionarItem(cenario.produto(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Quantidade deve ser positiva");
    }

    @Test
    void deveRejeitarProdutoDeOutroRestaurante() {
        Cenario cenario = cenarioPadrao();
        Restaurante outro = restauranteEm(-25.11, -50.17, 20);
        Produto produtoDeOutro = new Produto("Sopa", new BigDecimal("18.00"), true, outro);

        assertThatThrownBy(() -> cenario.pedido().adicionarItem(produtoDeOutro, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Produto pertence a outro restaurante");
    }

    @Test
    void deveImpedirConfirmacaoSemItens() {
        Cenario cenario = cenarioPadrao();
        assertThatThrownBy(cenario.pedido()::confirmar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Pedido deve possuir ao menos um item");
    }

    @Test
    void deveConfirmarEPagarPedido() {
        Cenario cenario = cenarioPadrao();
        cenario.pedido().adicionarItem(cenario.produto(), 1);
        cenario.pedido().confirmar();
        cenario.pedido().registrarPagamento(StatusPagamento.APROVADO);

        assertThat(cenario.pedido().getStatus()).isEqualTo(StatusPedido.PAGO);
        assertThatThrownBy(cenario.pedido()::confirmar)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deveRegistrarPagamentoRejeitado() {
        Cenario cenario = cenarioPadrao();
        cenario.pedido().adicionarItem(cenario.produto(), 1);
        cenario.pedido().confirmar();
        cenario.pedido().registrarPagamento(StatusPagamento.REJEITADO);
        assertThat(cenario.pedido().getStatus()).isEqualTo(StatusPedido.PAGAMENTO_REJEITADO);
    }

    @Test
    void deveExigirPedidoConfirmadoParaPagamento() {
        Cenario cenario = cenarioPadrao();
        assertThatThrownBy(() -> cenario.pedido().registrarPagamento(StatusPagamento.APROVADO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Pedido precisa estar confirmado para pagamento");
    }

    @Test
    void pagamentoTambemDeveClassificarRiscoDeLocalizacao() {
        Cenario cenario = cenarioPadrao();
        Pagamento seguro = new Pagamento(cenario.pedido(), FormaPagamento.PIX, StatusPagamento.APROVADO,
                BigDecimal.TEN, "P-1", "ok", "CENTRAL", 2.0);
        Pagamento arriscado = new Pagamento(cenario.pedido(), FormaPagamento.CARTAO_CREDITO,
                StatusPagamento.REJEITADO, BigDecimal.TEN, "P-2", "rejeitado", "EXTERNA", 20.0);

        assertThat(seguro.possuiRiscoGeografico()).isFalse();
        assertThat(arriscado.possuiRiscoGeografico()).isTrue();
        assertThat(arriscado.getRegiaoEntrega()).isEqualTo("EXTERNA");
        assertThat(arriscado.getDistanciaValidadaKm()).isEqualTo(20.0);
    }

    private Cenario cenarioPadrao() {
        Endereco endereco = enderecoEm(-25.095, -50.160);
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(endereco);
        Restaurante restaurante = restauranteEm(-25.100, -50.150, 20);
        Produto produto = new Produto("Pizza", new BigDecimal("25.00"), true, restaurante);
        return new Cenario(cliente, endereco, restaurante, produto,
                new Pedido(cliente, restaurante, endereco));
    }

    private Restaurante restauranteEm(double latitude, double longitude, double raio) {
        return new Restaurante("Restaurante", raio, enderecoEm(latitude, longitude));
    }

    private Endereco enderecoEm(double latitude, double longitude) {
        return new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(latitude, longitude));
    }

    private record Cenario(Cliente cliente, Endereco endereco, Restaurante restaurante,
                           Produto produto, Pedido pedido) {
    }
}
