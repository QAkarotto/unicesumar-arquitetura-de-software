package br.edu.foodnow.logistica;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.service.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PoliticaEntregaAtualTest {
    private final PoliticaEntregaAtual politica = new PoliticaEntregaAtual();

    @Test
    void devePreservarDistanciaTaxaEEstimativaInternasExtraidasDoPedido() {
        Pedido pedido = pedido("Centro", -25.095, -50.160, 20);
        assertThat(politica.distanciaInterna(pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega()))
                .isCloseTo(1.2410177275126, within(0.000000001));
        assertThat(politica.taxaInterna(pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega()))
                .isEqualByComparingTo("6.55");
        assertThat(politica.resumirPedido(pedido)).isEqualTo(new PoliticaEntregaAtual.ResumoPedido("CENTRAL", 21));
        assertThat(politica.adicionalDosItens(pedido)).isEqualByComparingTo("0.00");
    }

    @Test
    void deveArredondarAdicionalPorUnidadeEContarLinhasDeItensNoPrazo() {
        Pedido pedido = pedido("Uvaranas", -25.110, -50.180, 20);
        assertThat(politica.adicionalDosItens(pedido)).isEqualByComparingTo("0.50");
        assertThat(politica.resumirPedido(pedido).tempoMinutos()).isEqualTo(29);
        pedido.adicionarItem(pedido.getItens().getFirst().getProduto(), 1);
        assertThat(politica.adicionalDosItens(pedido)).isEqualByComparingTo("0.75");
        assertThat(politica.resumirPedido(pedido).tempoMinutos()).isEqualTo(31);
    }

    @Test
    void devePermitirOutroProvedorSemAlterarCasosDeUsoOuConhecerOClienteDeMapas() {
        ProvedorLocalizacao provedor = new ProvedorLocalizacao() {
            public Localizacao buscarCoordenadas(Endereco endereco) {
                return endereco.getLocalizacao();
            }

            public Rota calcularRota(Localizacao origem, Localizacao destino) {
                return new Rota(7.5, 60, "EXPANDIDA");
            }
        };
        LogisticaService logistica = new LogisticaService(provedor, politica);
        Pedido pedido = pedido("Centro", -25.095, -50.160, 20);
        var cotacao = logistica.cotarPedido(pedido.getCliente(), pedido.getRestaurante(), pedido.getEnderecoEntrega());
        assertThat(cotacao.distanciaKm()).isEqualTo(7.5);
        assertThat(cotacao.taxaBase()).isEqualByComparingTo("15.00");
        assertThat(logistica.planejarEntrega(pedido))
                .isEqualTo(new PoliticaEntregaAtual.PlanoEntrega(7.5, "EXPANDIDA", 60));
        assertThat(logistica.resumirPedido(pedido).regiao()).isEqualTo("CENTRAL");
        assertThat(logistica.adicionalDosItens(pedido)).isZero();
    }

    @Test
    void deveRejeitarDistanciaAcimaDoRaioSemSubstituirOErroAtual() {
        Pedido pedido = pedido("Centro", -25.095, -50.160, 0.1);
        assertThatThrownBy(() -> politica.cotarPedido(pedido.getCliente(), pedido.getRestaurante(),
                pedido.getEnderecoEntrega(), new Rota(1.32, 17, "CENTRAL")))
                .isInstanceOf(RegraNegocioException.class).hasMessage("Endereço fora da área de entrega");
    }

    @Test
    void devePreservarRegiaoNaoAtendidaNoResumoInterno() {
        Pedido original = pedido("Centro", -25.095, -50.160, 20);
        Endereco outro = new Endereco("Rua", "1", "Bairro", "Castro", "84100-000",
                new Localizacao(-25.095, -50.160));
        Pedido pedido = new Pedido(original.getCliente(), original.getRestaurante(), outro);
        assertThat(politica.resumirPedido(pedido).regiao()).isEqualTo("NAO_ATENDIDA");
    }

    private Pedido pedido(String bairro, double latitude, double longitude, double raio) {
        Endereco origem = new Endereco("Origem", "1", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.100, -50.150));
        Endereco destino = new Endereco("Destino", "2", bairro, "Ponta Grossa", "84030-000",
                new Localizacao(latitude, longitude));
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(destino);
        Restaurante restaurante = new Restaurante("Cantina", raio, origem);
        Pedido pedido = new Pedido(cliente, restaurante, destino);
        pedido.adicionarItem(new Produto("Pizza", new BigDecimal("30.00"), true, restaurante), 2);
        return pedido;
    }
}
