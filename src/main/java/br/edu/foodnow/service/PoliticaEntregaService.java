package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.util.TaxaEntregaUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Ponto único de decisão sobre distância, área atendida, taxa e tempo de entrega
 * usados na criação do pedido e no despacho da entrega. É também o limite onde a
 * futura regra de trajeto, região e horário deve ser incorporada, sem alterar
 * PedidoService ou EntregaService.
 */
@Service
public class PoliticaEntregaService {
    private final LocalizacaoService localizacaoService;
    private final FakeMapsClient mapsClient;

    public PoliticaEntregaService(LocalizacaoService localizacaoService, FakeMapsClient mapsClient) {
        this.localizacaoService = localizacaoService;
        this.mapsClient = mapsClient;
    }

    public AvaliacaoAreaEntrega avaliarParaNovoPedido(Cliente cliente, Restaurante restaurante,
                                                      Endereco enderecoEntrega) {
        double distancia = concentrarDistanciaCandidata(restaurante, enderecoEntrega);
        boolean atendido = distancia <= restaurante.getRaioEntregaKm()
                && cliente.estaDentroDaAreaDeEntrega(restaurante)
                && restaurante.atendeEndereco(enderecoEntrega);
        return new AvaliacaoAreaEntrega(distancia, atendido);
    }

    public BigDecimal calcularTaxaEntrega(Cliente cliente, Restaurante restaurante, Endereco enderecoEntrega,
                                          Pedido pedido, double distanciaKm) {
        return maiorTaxa(TaxaEntregaUtil.calcular(distanciaKm),
                restaurante.calcularTaxaEntrega(enderecoEntrega),
                restaurante.getEndereco().calcularTaxaLocalAte(enderecoEntrega),
                cliente.calcularTaxaEntregaDoRestaurante(restaurante),
                pedido.calcularTaxaEntregaPorDistancia());
    }

    public AvaliacaoDespacho avaliarParaDespacho(Pedido pedido) {
        Restaurante restaurante = pedido.getRestaurante();
        Endereco enderecoEntrega = pedido.getEnderecoEntrega();
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(
                restaurante.getEndereco().getLocalizacao(), enderecoEntrega.getLocalizacao());
        double distanciaEndereco = restaurante.getEndereco().calcularDistanciaAte(enderecoEntrega);
        double distanciaPedido = pedido.calcularDistanciaEntrega();
        double distancia = Math.max(rota.distanceKm(), Math.max(distanciaEndereco, distanciaPedido));
        String zona = "EXPANDIDA".equals(rota.deliveryZone())
                ? rota.deliveryZone() : enderecoEntrega.classificarZonaDeEntrega();
        int tempoBaseMinutos = Math.max(rota.durationMinutes(), pedido.estimarTempoEntregaPeloPedido());
        return new AvaliacaoDespacho(distancia, zona, tempoBaseMinutos);
    }

    private double concentrarDistanciaCandidata(Restaurante restaurante, Endereco enderecoEntrega) {
        Endereco enderecoRestaurante = restaurante.getEndereco();
        double distanciaDoService = localizacaoService.calcularDistancia(enderecoRestaurante, enderecoEntrega);
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(
                enderecoRestaurante.getLocalizacao(), enderecoEntrega.getLocalizacao());
        double distanciaDoRestaurante = restaurante.calcularDistanciaAte(enderecoEntrega);
        double distanciaDoEndereco = enderecoRestaurante.calcularDistanciaAte(enderecoEntrega);
        double distanciaManhattan = enderecoRestaurante.getLocalizacao()
                .calcularDistanciaManhattan(enderecoEntrega.getLocalizacao());
        return Math.max(distanciaDoService, Math.max(rota.distanceKm(),
                Math.max(distanciaDoRestaurante, Math.max(distanciaDoEndereco, distanciaManhattan))));
    }

    private BigDecimal maiorTaxa(BigDecimal... taxas) {
        BigDecimal maior = BigDecimal.ZERO;
        for (BigDecimal taxa : taxas) {
            if (taxa.compareTo(maior) > 0) {
                maior = taxa;
            }
        }
        return maior;
    }

    public record AvaliacaoAreaEntrega(double distanciaKm, boolean atendido) {
    }

    public record AvaliacaoDespacho(double distanciaKm, String zona, int tempoBaseMinutos) {
    }
}
