package br.edu.foodnow.localizacao;

import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.util.TaxaEntregaUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de caracterização do limite de localização.
 *
 * <p>Fixam as decisões que antes existiam apenas como expressão dentro do caso de uso, a
 * reconciliação de distância pela maior das fórmulas, a taxa pela maior das cinco e os três
 * critérios independentes de atendimento.</p>
 */
class LocalizacaoTest {
    private final MapaGateway mapaGateway = new MapaGatewayFake(new FakeMapsClient());
    private final CalculadoraRotaEntrega calculadora = new CalculadoraRotaEntrega(mapaGateway);
    private final PoliticaAreaAtendimento politicaArea = new PoliticaAreaAtendimento();
    private final PoliticaTaxaEntrega politicaTaxa = new PoliticaTaxaEntrega();

    @Test
    void adaptadorDeveTraduzirCoordenadasEPrecisaoDoProvedor() {
        Geocodificacao exata = mapaGateway.geocodificar(enderecoEm(-25.100, -50.150));
        Geocodificacao aproximada = mapaGateway.geocodificar(new Endereco("Rua Sem Coordenada", "1",
                "Centro", "Ponta Grossa", "84000-000", null));

        assertThat(exata.aproximada()).isFalse();
        assertThat(exata.localizacao().getLatitude()).isEqualTo(-25.100);
        assertThat(exata.localizacao().getLongitude()).isEqualTo(-50.150);
        assertThat(aproximada.aproximada()).isTrue();
    }

    @Test
    void adaptadorDeveEntregarRotaSemVocabularioDoProvedor() {
        Rota rota = mapaGateway.calcularRota(new Localizacao(-25.100, -50.150),
                new Localizacao(-25.095, -50.160));

        assertThat(rota.distanciaKm()).isPositive();
        assertThat(rota.duracaoMinutos()).isGreaterThan(12);
        assertThat(rota.regiao()).isEqualTo("CENTRAL");
    }

    @Test
    void rotaDoPedidoDeveAdotarAMaiorEntreAsFormulasConcorrentes() {
        Restaurante restaurante = restauranteEm(-25.100, -50.150, 20);
        Endereco destino = enderecoEm(-25.095, -50.160);
        Endereco origem = restaurante.getEndereco();

        RotaEntrega rota = calculadora.paraCriacaoDePedido(restaurante, destino);

        double trajetoDoProvedor = rota.distanciaProvedorKm();
        double haversine = restaurante.calcularDistanciaAte(destino);
        double cartesiana = origem.calcularDistanciaAte(destino);
        double manhattan = origem.getLocalizacao().calcularDistanciaManhattan(destino.getLocalizacao());

        assertThat(rota.distanciaKm())
                .isEqualTo(Math.max(trajetoDoProvedor, Math.max(haversine,
                        Math.max(cartesiana, manhattan))));
        assertThat(rota.distanciaKm()).isGreaterThan(haversine);
        assertThat(rota.regiaoProvedor()).isEqualTo("CENTRAL");
    }

    @Test
    void rotaDoDespachoDeveAdotarAMaiorEntreProvedorEnderecoEPedido() {
        Pedido pedido = pedidoPadrao(20);
        Endereco origem = pedido.getRestaurante().getEndereco();
        Endereco destino = pedido.getEnderecoEntrega();

        RotaEntrega rota = calculadora.paraDespachoDeEntrega(pedido);

        assertThat(rota.distanciaKm()).isEqualTo(Math.max(rota.distanciaProvedorKm(),
                Math.max(origem.calcularDistanciaAte(destino), pedido.calcularDistanciaEntrega())));
        assertThat(rota.duracaoProvedorMinutos()).isGreaterThan(12);
    }

    @Test
    void regiaoDeDespachoDevePreferirZonaDoEnderecoSalvoQuandoProvedorDizExpandida() {
        Endereco proximo = enderecoEm(-25.095, -50.160);
        Endereco distante = new Endereco("Rua Longe", "5", "Uvaranas", "Ponta Grossa", "84030-000",
                new Localizacao(-25.400, -50.500));

        RotaEntrega central = calculadora.paraCriacaoDePedido(restauranteEm(-25.100, -50.150, 20), proximo);
        RotaEntrega expandida = calculadora.paraCriacaoDePedido(restauranteEm(-25.100, -50.150, 200), distante);

        assertThat(central.regiaoProvedor()).isEqualTo("CENTRAL");
        assertThat(calculadora.regiaoDeDespacho(central, proximo)).isEqualTo("CENTRAL");
        assertThat(expandida.regiaoProvedor()).isEqualTo("EXPANDIDA");
        assertThat(calculadora.regiaoDeDespacho(expandida, distante)).isEqualTo("EXPANDIDA");
        assertThat(distante.classificarZonaDeEntrega()).isEqualTo("URBANA");
    }

    @Test
    void taxaDeveAdotarAMaiorEntreAsCincoFormulas() {
        Pedido pedido = pedidoPadrao(20);
        double distancia = calculadora.paraCriacaoDePedido(pedido.getRestaurante(),
                pedido.getEnderecoEntrega()).distanciaKm();
        Endereco destino = pedido.getEnderecoEntrega();
        Restaurante restaurante = pedido.getRestaurante();

        BigDecimal base = politicaTaxa.calcularBase(pedido, distancia);

        assertThat(base)
                .isGreaterThanOrEqualTo(TaxaEntregaUtil.calcular(distancia))
                .isGreaterThanOrEqualTo(restaurante.calcularTaxaEntrega(destino))
                .isGreaterThanOrEqualTo(restaurante.getEndereco().calcularTaxaLocalAte(destino))
                .isGreaterThanOrEqualTo(pedido.getCliente().calcularTaxaEntregaDoRestaurante(restaurante))
                .isGreaterThanOrEqualTo(pedido.calcularTaxaEntregaPorDistancia());
    }

    @Test
    void taxaComItensDeveSomarOAdicionalGeograficoDoPedido() {
        Pedido pedido = pedidoPadrao(20);
        Endereco foraDoCentro = new Endereco("Rua Uvaranas", "9", "Uvaranas", "Ponta Grossa",
                "84030-000", new Localizacao(-25.110, -50.180));
        Pedido pedidoRegional = new Pedido(pedido.getCliente(), pedido.getRestaurante(), foraDoCentro);
        pedidoRegional.adicionarItem(new Produto("Pizza", new BigDecimal("25.00"), true,
                pedido.getRestaurante()), 3);

        BigDecimal base = politicaTaxa.calcularBase(pedidoRegional, 4.0);
        BigDecimal comItens = politicaTaxa.calcularComAdicionalDosItens(pedidoRegional, 4.0);

        assertThat(pedidoRegional.calcularAdicionalGeograficoDosItens()).isPositive();
        assertThat(comItens).isEqualByComparingTo(
                base.add(pedidoRegional.calcularAdicionalGeograficoDosItens()));
    }

    @Test
    void areaDeAtendimentoDeveRecusarPeloRaioMesmoComHaversineDentroDoLimite() {
        Restaurante restaurante = restauranteEm(-25.100, -50.150, 1.3);
        Endereco destino = enderecoEm(-25.095, -50.160);
        Cliente cliente = clienteCom(destino);
        double reconciliada = calculadora.paraCriacaoDePedido(restaurante, destino).distanciaKm();

        assertThat(reconciliada).isGreaterThan(1.3);
        assertThat(cliente.calcularDistanciaAte(restaurante)).isLessThan(1.3);
        assertThat(cliente.estaDentroDaAreaDeEntrega(restaurante)).isTrue();
        assertThat(politicaArea.atende(cliente, restaurante, destino, reconciliada)).isFalse();
    }

    @Test
    void areaDeAtendimentoDeveRecusarQuandoEnderecoPrincipalDoClienteEstaLonge() {
        Restaurante restaurante = restauranteEm(-25.100, -50.150, 50);
        Endereco destino = enderecoEm(-25.095, -50.160);
        Cliente cliente = clienteCom(enderecoEm(-25.500, -50.500));
        double reconciliada = calculadora.paraCriacaoDePedido(restaurante, destino).distanciaKm();

        assertThat(reconciliada).isLessThan(50);
        assertThat(politicaArea.restauranteCobre(restaurante, destino)).isTrue();
        assertThat(cliente.estaDentroDaAreaDeEntrega(restaurante)).isFalse();
        assertThat(politicaArea.atende(cliente, restaurante, destino, reconciliada)).isFalse();
    }

    @Test
    void areaDeAtendimentoDeveRecusarQuandoRestauranteNaoCobreARegiao() {
        Restaurante restaurante = restauranteEm(-25.100, -50.150, 200);
        Endereco destino = new Endereco("Rua Externa", "2", "Bairro", "Castro", "84160-000",
                new Localizacao(-24.790, -50.010));
        Cliente cliente = clienteCom(destino);
        double reconciliada = calculadora.paraCriacaoDePedido(restaurante, destino).distanciaKm();

        assertThat(reconciliada).isLessThan(200);
        assertThat(cliente.estaDentroDaAreaDeEntrega(restaurante)).isTrue();
        assertThat(politicaArea.restauranteCobre(restaurante, destino)).isFalse();
        assertThat(politicaArea.atende(cliente, restaurante, destino, reconciliada)).isFalse();
    }

    @Test
    void areaDeAtendimentoDeveAceitarQuandoOsTresCriteriosPassam() {
        Restaurante restaurante = restauranteEm(-25.100, -50.150, 20);
        Endereco destino = enderecoEm(-25.095, -50.160);
        Cliente cliente = clienteCom(destino);
        double reconciliada = calculadora.paraCriacaoDePedido(restaurante, destino).distanciaKm();

        assertThat(politicaArea.atende(cliente, restaurante, destino, reconciliada)).isTrue();
    }

    @Test
    void planejamentoDeveEntregarRotaAtendimentoETaxaEmUmResultadoSo() {
        PlanejamentoDeEntrega planejamento = new PlanejamentoDeEntrega(calculadora, politicaArea,
                politicaTaxa);
        Restaurante restaurante = restauranteEm(-25.100, -50.150, 20);
        Endereco destino = enderecoEm(-25.095, -50.160);
        Cliente cliente = clienteCom(destino);
        Produto produto = new Produto("Pizza", new BigDecimal("25.00"), true, restaurante);

        PlanoDeEntrega plano = planejamento.planejar(cliente, restaurante, destino);
        Pedido pedido = new Pedido(cliente, restaurante, destino);
        planejamento.aplicarA(pedido, plano);
        BigDecimal taxaBase = pedido.getTaxaEntrega();
        pedido.adicionarItem(produto, 2);
        planejamento.reaplicarComItens(pedido, plano);

        assertThat(plano.atendido()).isTrue();
        assertThat(plano.distanciaKm()).isEqualTo(plano.rota().distanciaKm());
        assertThat(pedido.getDistanciaEntregaKm()).isEqualTo(plano.distanciaKm());
        assertThat(pedido.getTaxaEntrega()).isEqualByComparingTo(taxaBase);
        assertThat(pedido.getValorTotal()).isEqualByComparingTo(pedido.getSubtotal().add(taxaBase));
        assertThat(planejamento.produtoAlcanca(produto, destino)).isTrue();
    }

    private Pedido pedidoPadrao(double raio) {
        Endereco destino = enderecoEm(-25.095, -50.160);
        Cliente cliente = clienteCom(destino);
        return new Pedido(cliente, restauranteEm(-25.100, -50.150, raio), destino);
    }

    private Cliente clienteCom(Endereco endereco) {
        Cliente cliente = new Cliente("Ana", "ana@foodnow.test");
        cliente.adicionarEndereco(endereco);
        return cliente;
    }

    private Restaurante restauranteEm(double latitude, double longitude, double raio) {
        return new Restaurante("Restaurante", raio, enderecoEm(latitude, longitude));
    }

    private Endereco enderecoEm(double latitude, double longitude) {
        return new Endereco("Rua Teste", "10", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(latitude, longitude));
    }
}
