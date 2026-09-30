package br.edu.foodnow.entrega;

import br.edu.foodnow.geo.ProvedorGeografico;
import br.edu.foodnow.geo.Rota;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.util.TaxaEntregaUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Implementação atual (equivalente ao comportamento anterior à refatoração) das regras de
 * entrega. Concentra o que antes estava espalhado em {@code PedidoService},
 * {@code EntregaService}, {@code PagamentoService}, {@code NotificacaoService},
 * {@code Pedido}, {@code Produto} e {@code ItemPedido}.
 * <p>
 * Depende apenas da porta {@link ProvedorGeografico}, nunca de um cliente de mapas concreto.
 */
@Component
public class PoliticaEntregaPadrao implements PoliticaEntrega {
    private static final String ZONA_EXPANDIDA = "EXPANDIDA";
    private static final String ZONA_CENTRAL = "CENTRAL";
    private static final String REGIAO_NAO_ATENDIDA = "NAO_ATENDIDA";

    private final ProvedorGeografico provedorGeografico;

    public PoliticaEntregaPadrao(ProvedorGeografico provedorGeografico) {
        this.provedorGeografico = provedorGeografico;
    }

    @Override
    public CotacaoEntrega cotar(SolicitacaoCotacao solicitacao) {
        Cliente cliente = solicitacao.cliente();
        Restaurante restaurante = solicitacao.restaurante();
        Endereco destino = solicitacao.destino();

        Rota rota = provedorGeografico.calcularRota(restaurante.getEndereco().getLocalizacao(),
                destino.getLocalizacao());
        double distancia = distanciaEfetiva(restaurante, destino, rota);
        boolean atendido = distancia <= restaurante.getRaioEntregaKm()
                && cliente.estaDentroDaAreaDeEntrega(restaurante)
                && restaurante.atendeEndereco(destino);
        if (!atendido) {
            return CotacaoEntrega.foraDaArea(distancia);
        }
        return new CotacaoEntrega(distancia, true, maiorTaxa(
                TaxaEntregaUtil.calcular(distancia),
                restaurante.calcularTaxaEntrega(destino),
                restaurante.getEndereco().calcularTaxaLocalAte(destino),
                cliente.calcularTaxaEntregaDoRestaurante(restaurante),
                taxaPorDistanciaPlana(restaurante, destino)));
    }

    @Override
    public BigDecimal taxaFinal(CotacaoEntrega cotacao, Pedido pedido) {
        return cotacao.taxaBase().add(adicionalGeograficoDosItens(pedido));
    }

    @Override
    public AvaliacaoGeografica avaliar(Pedido pedido) {
        return new AvaliacaoGeografica(rotaDoPedido(pedido), regiaoDeEntrega(pedido));
    }

    @Override
    public String regiaoDeEntrega(Pedido pedido) {
        Endereco destino = pedido.getEnderecoEntrega();
        if (!pedido.getRestaurante().getEndereco().pertenceARegiaoDo(destino)) {
            return REGIAO_NAO_ATENDIDA;
        }
        return destino.classificarZonaDeEntrega();
    }

    @Override
    public int estimarTempoInterno(Pedido pedido) {
        double distancia = distanciaPlana(pedido.getRestaurante(), pedido.getEnderecoEntrega());
        return 14 + (int) Math.ceil(distancia * 3.6) + pedido.getItens().size() * 2;
    }

    @Override
    public PlanoEntrega planejarEntrega(Pedido pedido) {
        Restaurante restaurante = pedido.getRestaurante();
        Endereco destino = pedido.getEnderecoEntrega();
        Rota rota = rotaDoPedido(pedido);

        double distanciaDoEndereco = restaurante.getEndereco().calcularDistanciaAte(destino);
        double distancia = Math.max(rota.distanciaKm(),
                Math.max(distanciaDoEndereco, distanciaPlana(restaurante, destino)));
        String zona = ZONA_EXPANDIDA.equals(rota.zona()) ? rota.zona() : destino.classificarZonaDeEntrega();
        int tempoBase = Math.max(rota.duracaoMinutos(), estimarTempoInterno(pedido));
        return new PlanoEntrega(distancia, zona, tempoBase);
    }

    private Rota rotaDoPedido(Pedido pedido) {
        return provedorGeografico.calcularRota(pedido.getRestaurante().getEndereco().getLocalizacao(),
                pedido.getEnderecoEntrega().getLocalizacao());
    }

    /** Maior entre a rota do provedor e as três medidas locais (haversine, plana por endereço, Manhattan). */
    private double distanciaEfetiva(Restaurante restaurante, Endereco destino, Rota rota) {
        double haversine = restaurante.calcularDistanciaAte(destino);
        double planaPorEndereco = restaurante.getEndereco().calcularDistanciaAte(destino);
        double manhattan = restaurante.getEndereco().getLocalizacao()
                .calcularDistanciaManhattan(destino.getLocalizacao());
        return Math.max(rota.distanciaKm(), Math.max(haversine, Math.max(planaPorEndereco, manhattan)));
    }

    /** Aproximação plana (111 km por grau) que o pedido usava para taxa e tempo. */
    private double distanciaPlana(Restaurante restaurante, Endereco destino) {
        double diferencaLatitude = restaurante.buscarLatitude() - destino.getLocalizacao().getLatitude();
        double diferencaLongitude = restaurante.buscarLongitude() - destino.getLocalizacao().getLongitude();
        return Math.sqrt(diferencaLatitude * diferencaLatitude + diferencaLongitude * diferencaLongitude) * 111.0;
    }

    private BigDecimal taxaPorDistanciaPlana(Restaurante restaurante, Endereco destino) {
        return BigDecimal.valueOf(5.00 + distanciaPlana(restaurante, destino) * 1.25)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal adicionalGeograficoDosItens(Pedido pedido) {
        Endereco destino = pedido.getEnderecoEntrega();
        return pedido.getItens().stream()
                .map(item -> adicionalRegional(item.getProduto(), destino)
                        .multiply(BigDecimal.valueOf(item.getQuantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal adicionalRegional(Produto produto, Endereco destino) {
        double distancia = produto.getRestaurante().getEndereco().calcularDistanciaAte(destino);
        double adicional = ZONA_CENTRAL.equals(destino.classificarZonaDeEntrega()) ? 0.0 : distancia * 0.08;
        return BigDecimal.valueOf(adicional).setScale(2, RoundingMode.HALF_UP);
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
}
