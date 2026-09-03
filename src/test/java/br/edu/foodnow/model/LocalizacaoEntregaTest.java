package br.edu.foodnow.model;

import br.edu.foodnow.util.DistanciaUtil;
import br.edu.foodnow.util.TaxaEntregaUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalizacaoEntregaTest {

    @Test
    void deveValidarLimitesDasCoordenadas() {
        assertThatThrownBy(() -> new Localizacao(-91, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Coordenadas inválidas");
        assertThatThrownBy(() -> new Localizacao(0, 181))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveCalcularDistanciaConhecida() {
        Localizacao origem = new Localizacao(-25.100, -50.150);
        Localizacao destino = new Localizacao(-25.095, -50.160);
        assertThat(DistanciaUtil.calcularKm(origem, destino)).isBetween(1.0, 1.3);
    }

    @Test
    void deveAplicarFaixasDeTaxa() {
        assertThat(TaxaEntregaUtil.calcular(2)).isEqualByComparingTo("6.90");
        assertThat(TaxaEntregaUtil.calcular(5)).isEqualByComparingTo("12.00");
    }

    @Test
    void clienteERestauranteTambemCalculamLocalizacao() {
        Endereco clienteEndereco = endereco(-25.095, -50.160);
        Cliente cliente = new Cliente("Bia", "bia@foodnow.test");
        cliente.adicionarEndereco(clienteEndereco);
        Restaurante restaurante = new Restaurante("Massa", 10, endereco(-25.100, -50.150));

        assertThat(cliente.calcularDistanciaAte(restaurante)).isPositive();
        assertThat(cliente.estaDentroDaAreaDeEntrega(restaurante)).isTrue();
        assertThat(restaurante.calcularDistanciaAte(clienteEndereco)).isPositive();
        assertThat(restaurante.calcularTaxaEntrega(clienteEndereco)).isGreaterThan(BigDecimal.valueOf(4));
        assertThat(restaurante.buscarLatitude()).isEqualTo(-25.100);
        assertThat(restaurante.buscarLongitude()).isEqualTo(-50.150);
    }

    @Test
    void deveDefinirEnderecoPrincipal() throws Exception {
        Cliente cliente = new Cliente("Caio", "caio@foodnow.test");
        Endereco primeiro = endereco(-25.1, -50.1);
        Endereco segundo = endereco(-25.2, -50.2);
        cliente.adicionarEndereco(primeiro);
        cliente.adicionarEndereco(segundo);
        definirId(segundo, 7L);

        cliente.definirEnderecoPrincipal(7L);
        assertThat(cliente.enderecoPrincipal()).isSameAs(segundo);
        assertThat(primeiro.isPrincipal()).isFalse();
        assertThatThrownBy(() -> cliente.definirEnderecoPrincipal(99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void entregaDeveCalcularEstimativaEAtualizarStatus() {
        Cliente cliente = new Cliente("Dani", "dani@foodnow.test");
        Endereco destino = endereco(-25.095, -50.160);
        cliente.adicionarEndereco(destino);
        Restaurante restaurante = new Restaurante("Lanche", 20, endereco(-25.100, -50.150));
        Pedido pedido = new Pedido(cliente, restaurante, destino);
        Entrega entrega = new Entrega(pedido, destino, 1.3, 18);

        assertThat(entrega.calcularDistancia()).isPositive();
        assertThat(entrega.estimarTempoEntrega()).isGreaterThan(15);
        entrega.atualizarStatus(StatusEntrega.EM_ROTA);
        assertThat(entrega.getStatus()).isEqualTo(StatusEntrega.EM_ROTA);
        assertThat(entrega.getDistanciaKm()).isEqualTo(1.3);
        assertThat(entrega.getTempoEstimadoMinutos()).isEqualTo(18);
    }

    private Endereco endereco(double latitude, double longitude) {
        return new Endereco("Rua A", "1", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(latitude, longitude));
    }

    private void definirId(Endereco endereco, Long id) throws Exception {
        var campo = Endereco.class.getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(endereco, id);
    }
}
