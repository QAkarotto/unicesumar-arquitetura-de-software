package br.edu.foodnow.logistica;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.service.RegraNegocioException;
import br.edu.foodnow.util.TaxaEntregaUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Reúne as decisões logísticas vigentes, preservando suas fórmulas e diferenças observáveis. */
@Component
public class PoliticaEntregaAtual {
    public CotacaoPedido cotarPedido(Cliente cliente, Restaurante restaurante, Endereco destino, Rota rota) {
        double distancia = Math.max(rota.distanciaKm(), Math.max(restaurante.calcularDistanciaAte(destino),
                Math.max(restaurante.getEndereco().calcularDistanciaAte(destino),
                        restaurante.getEndereco().getLocalizacao().calcularDistanciaManhattan(destino.getLocalizacao()))));
        // O endereço principal participa da regra atual mesmo quando o pedido usa outro endereço.
        if (distancia > restaurante.getRaioEntregaKm() || !cliente.estaDentroDaAreaDeEntrega(restaurante)
                || !restaurante.atendeEndereco(destino)) {
            throw new RegraNegocioException("Endereço fora da área de entrega");
        }
        BigDecimal taxa = maiorTaxa(TaxaEntregaUtil.calcular(distancia),
                restaurante.calcularTaxaEntrega(destino), restaurante.getEndereco().calcularTaxaLocalAte(destino),
                cliente.calcularTaxaEntregaDoRestaurante(restaurante), taxaInterna(restaurante.getEndereco(), destino));
        return new CotacaoPedido(distancia, taxa);
    }

    public BigDecimal adicionalDosItens(Pedido pedido) {
        // Arredondar cada adicional unitário antes de multiplicar mantém os centavos do contrato atual.
        return pedido.getItens().stream().map(item -> {
            double distancia = item.getProduto().getRestaurante().getEndereco()
                    .calcularDistanciaAte(pedido.getEnderecoEntrega());
            double adicional = "CENTRAL".equals(pedido.getEnderecoEntrega().classificarZonaDeEntrega())
                    ? 0.0 : distancia * 0.08;
            return BigDecimal.valueOf(adicional).setScale(2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(item.getQuantidade()));
        }).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    public PlanoEntrega planejarEntrega(Pedido pedido, Rota rota) {
        Endereco origem = pedido.getRestaurante().getEndereco();
        Endereco destino = pedido.getEnderecoEntrega();
        // A distância do despacho não inclui Manhattan, ao contrário da cotação do pedido.
        double distancia = Math.max(rota.distanciaKm(), Math.max(origem.calcularDistanciaAte(destino),
                distanciaInterna(origem, destino)));
        String zona = "EXPANDIDA".equals(rota.zona()) ? rota.zona() : destino.classificarZonaDeEntrega();
        int tempo = Math.max(rota.tempoMinutos(), tempoInterno(pedido));
        return new PlanoEntrega(distancia, zona, tempo);
    }

    public ResumoPedido resumirPedido(Pedido pedido) {
        String regiao = pedido.getRestaurante().getEndereco().pertenceARegiaoDo(pedido.getEnderecoEntrega())
                ? pedido.getEnderecoEntrega().classificarZonaDeEntrega() : "NAO_ATENDIDA";
        return new ResumoPedido(regiao, tempoInterno(pedido));
    }

    double distanciaInterna(Endereco origem, Endereco destino) {
        double latitude = origem.getLocalizacao().getLatitude() - destino.getLocalizacao().getLatitude();
        double longitude = origem.getLocalizacao().getLongitude() - destino.getLocalizacao().getLongitude();
        return Math.sqrt(latitude * latitude + longitude * longitude) * 111.0;
    }

    BigDecimal taxaInterna(Endereco origem, Endereco destino) {
        return BigDecimal.valueOf(5.00 + distanciaInterna(origem, destino) * 1.25)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private int tempoInterno(Pedido pedido) {
        return 14 + (int) Math.ceil(distanciaInterna(pedido.getRestaurante().getEndereco(),
                pedido.getEnderecoEntrega()) * 3.6) + pedido.getItens().size() * 2;
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

    public record CotacaoPedido(double distanciaKm, BigDecimal taxaBase) {
    }

    public record PlanoEntrega(double distanciaKm, String zona, int tempoBaseMinutos) {
    }

    public record ResumoPedido(String regiao, int tempoMinutos) {
    }
}
