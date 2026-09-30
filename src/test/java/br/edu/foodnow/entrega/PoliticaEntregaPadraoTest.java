package br.edu.foodnow.entrega;

import br.edu.foodnow.geo.CoordenadasResolvidas;
import br.edu.foodnow.geo.ProvedorGeografico;
import br.edu.foodnow.geo.Rota;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Testa a política de entrega sem Spring e sem o cliente de mapas concreto: o provedor
 * geográfico é substituído por uma implementação fixa da porta {@link ProvedorGeografico}.
 * Os valores esperados foram obtidos executando o código anterior à refatoração.
 */
class PoliticaEntregaPadraoTest {
    private static final double KM = 1e-6;

    private final AtomicInteger consultasDeRota = new AtomicInteger();

    // ---------- cotar ----------

    @Test
    void cotarDeveUsarAMaiorDistanciaEntreProvedorEMedidasLocais() {
        Cenario central = cenarioCentral();

        CotacaoEntrega semAjudaDoProvedor = politica(new Rota(0.0, 10, "CENTRAL")).cotar(central.solicitacao());
        assertThat(semAjudaDoProvedor.atendido()).isTrue();
        assertThat(semAjudaDoProvedor.distanciaKm()).isCloseTo(1.515, within(KM));
        assertThat(semAjudaDoProvedor.taxaBase()).isEqualByComparingTo("6.55");

        CotacaoEntrega provedorMaior = politica(new Rota(3.0, 10, "CENTRAL")).cotar(central.solicitacao());
        assertThat(provedorMaior.distanciaKm()).isEqualTo(3.0);
    }

    @Test
    void cotarDeveRecusarQuandoADistanciaDoProvedorExcedeOAtendimento() {
        Cenario central = cenarioCentral();

        CotacaoEntrega cotacao = politica(new Rota(50.0, 90, "EXPANDIDA")).cotar(central.solicitacao());

        assertThat(cotacao.atendido()).isFalse();
        assertThat(cotacao.distanciaKm()).isEqualTo(50.0);
        assertThat(cotacao.taxaBase()).isEqualByComparingTo("0");
    }

    @Test
    void cotarDeveRecusarEnderecoDeOutraRegiao() {
        Endereco restauranteEndereco = endereco("Centro", "Ponta Grossa", "84000-000", -25.100, -50.150);
        Restaurante restaurante = new Restaurante("Cantina", 60, restauranteEndereco);
        Endereco outraCidade = endereco("Centro", "Castro", "84160-000", -25.095, -50.160);
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(outraCidade);

        CotacaoEntrega cotacao = politica(new Rota(1.0, 10, "CENTRAL"))
                .cotar(new SolicitacaoCotacao(cliente, restaurante, outraCidade));

        assertThat(cotacao.atendido()).isFalse();
    }

    // ---------- taxa final ----------

    @Test
    void taxaFinalNaZonaCentralNaoDeveTerAdicionalDeItens() {
        Cenario central = cenarioCentral();
        PoliticaEntregaPadrao politica = politica(new Rota(1.32, 17, "CENTRAL"));
        CotacaoEntrega cotacao = politica.cotar(central.solicitacao());

        assertThat(politica.taxaFinal(cotacao, central.pedidoComItem())).isEqualByComparingTo("6.55");
    }

    @Test
    void taxaFinalForaDoCentroDeveSomarAdicionalPorItem() {
        Cenario urbano = cenarioUrbano();
        PoliticaEntregaPadrao politica = politica(new Rota(8.62, 43, "EXPANDIDA"));
        CotacaoEntrega cotacao = politica.cotar(urbano.solicitacao());

        assertThat(cotacao.distanciaKm()).isCloseTo(10.35, within(KM));
        assertThat(cotacao.taxaBase()).isEqualByComparingTo("18.42");
        assertThat(politica.taxaFinal(cotacao, urbano.pedidoComItem())).isEqualByComparingTo("19.60");
    }

    // ---------- região, tempo e despacho ----------

    @Test
    void regiaoDeEntregaDeveClassificarCentralUrbanaENaoAtendida() {
        PoliticaEntregaPadrao politica = politica(new Rota(1.0, 10, "CENTRAL"));
        assertThat(politica.regiaoDeEntrega(cenarioCentral().pedidoComItem())).isEqualTo("CENTRAL");
        assertThat(politica.regiaoDeEntrega(cenarioUrbano().pedidoComItem())).isEqualTo("URBANA");

        Restaurante restaurante = new Restaurante("Cantina", 60,
                endereco("Centro", "Ponta Grossa", "84000-000", -25.100, -50.150));
        Endereco outraCidade = endereco("Centro", "Castro", "84160-000", -25.095, -50.160);
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(outraCidade);
        assertThat(politica.regiaoDeEntrega(new Pedido(cliente, restaurante, outraCidade))).isEqualTo("NAO_ATENDIDA");
    }

    @Test
    void estimarTempoInternoDeveConsiderarDistanciaPlanaEQuantidadeDeItens() {
        PoliticaEntregaPadrao politica = politica(new Rota(1.0, 10, "CENTRAL"));
        assertThat(politica.estimarTempoInterno(cenarioCentral().pedidoComItem())).isEqualTo(21);
        assertThat(politica.estimarTempoInterno(cenarioUrbano().pedidoComItem())).isEqualTo(45);
    }

    @Test
    void avaliarDeveDevolverRotaDoProvedorERegiaoConsultandoORotaUmaVez() {
        Rota rota = new Rota(8.62, 43, "EXPANDIDA");
        PoliticaEntregaPadrao politica = politica(rota);

        AvaliacaoGeografica avaliacao = politica.avaliar(cenarioUrbano().pedidoComItem());

        assertThat(avaliacao.rota()).isEqualTo(rota);
        assertThat(avaliacao.regiao()).isEqualTo("URBANA");
        assertThat(consultasDeRota.get()).isEqualTo(1);
    }

    @Test
    void planejarEntregaCentralDeveUsarRotaEMaiorTempo() {
        PoliticaEntregaPadrao politica = politica(new Rota(1.32, 17, "CENTRAL"));

        PlanoEntrega plano = politica.planejarEntrega(cenarioCentral().pedidoComItem());

        assertThat(plano.distanciaKm()).isEqualTo(1.32);
        assertThat(plano.zona()).isEqualTo("CENTRAL");
        assertThat(plano.tempoBaseMinutos()).isEqualTo(21);
        assertThat(plano.tempoTotalMinutos(4)).isEqualTo(25);
    }

    @Test
    void planejarEntregaComZonaExpandidaDoProvedorDeveManterZonaExpandida() {
        PoliticaEntregaPadrao politica = politica(new Rota(8.62, 43, "EXPANDIDA"));

        PlanoEntrega plano = politica.planejarEntrega(cenarioUrbano().pedidoComItem());

        assertThat(plano.distanciaKm()).isEqualTo(8.62);
        assertThat(plano.zona()).isEqualTo("EXPANDIDA");
        assertThat(plano.tempoBaseMinutos()).isEqualTo(45);
        assertThat(plano.tempoTotalMinutos(9)).isEqualTo(54);
    }

    @Test
    void planejarEntregaSemZonaExpandidaDeveClassificarPeloEndereco() {
        PoliticaEntregaPadrao politica = politica(new Rota(1.0, 10, "CENTRAL"));

        PlanoEntrega plano = politica.planejarEntrega(cenarioUrbano().pedidoComItem());

        assertThat(plano.zona()).isEqualTo("URBANA");
    }

    // ---------- apoio ----------

    private PoliticaEntregaPadrao politica(Rota rotaFixa) {
        ProvedorGeografico provedor = new ProvedorGeografico() {
            @Override
            public CoordenadasResolvidas buscarCoordenadas(Endereco endereco) {
                throw new UnsupportedOperationException("não usado pela política");
            }

            @Override
            public Rota calcularRota(Localizacao origem, Localizacao destino) {
                consultasDeRota.incrementAndGet();
                return rotaFixa;
            }
        };
        return new PoliticaEntregaPadrao(provedor);
    }

    private Cenario cenarioCentral() {
        return cenario(endereco("Centro", "Ponta Grossa", "84000-000", -25.095, -50.160));
    }

    private Cenario cenarioUrbano() {
        return cenario(endereco("Uvaranas", "Ponta Grossa", "84010-100", -25.150, -50.200));
    }

    private Cenario cenario(Endereco destino) {
        Restaurante restaurante = new Restaurante("Cantina", 25,
                endereco("Centro", "Ponta Grossa", "84000-000", -25.100, -50.150));
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(destino);
        Produto produto = new Produto("Pizza", new BigDecimal("30.00"), true, restaurante);
        return new Cenario(cliente, restaurante, destino, produto);
    }

    private Endereco endereco(String bairro, String cidade, String cep, double latitude, double longitude) {
        return new Endereco("Rua Teste", "10", bairro, cidade, cep, new Localizacao(latitude, longitude));
    }

    private record Cenario(Cliente cliente, Restaurante restaurante, Endereco destino, Produto produto) {
        SolicitacaoCotacao solicitacao() {
            return new SolicitacaoCotacao(cliente, restaurante, destino);
        }

        Pedido pedidoComItem() {
            Pedido pedido = new Pedido(cliente, restaurante, destino);
            pedido.adicionarItem(produto, 2);
            return pedido;
        }
    }
}
