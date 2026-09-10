package br.edu.foodnow.service;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class CalculoFrete {

    public BigDecimal calcularTaxaEntrega(double distanciaKm) {
        BigDecimal base = distanciaKm <= 3.0 ? BigDecimal.valueOf(4.50) : BigDecimal.valueOf(6.00);
        return base.add(BigDecimal.valueOf(distanciaKm).multiply(BigDecimal.valueOf(1.20)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularMaiorTaxa(BigDecimal... taxas) {
        BigDecimal maior = BigDecimal.ZERO;
        for (BigDecimal taxa : taxas) {
            if (taxa.compareTo(maior) > 0) {
                maior = taxa;
            }
        }
        return maior;
    }

    public BigDecimal calcularTaxaRestaurante(Restaurante restaurante, Endereco destino) {
        double distancia = calcularDistancia(restaurante.getEndereco(), destino);
        return BigDecimal.valueOf(4.00 + distancia * 1.35).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularTaxaEndereco(Endereco origem, Endereco destino) {
        double distancia = calcularDistancia(origem, destino);
        double adicionalRegiao = origem.pertenceARegiaoDo(destino) ? 0.0 : 3.50;
        return BigDecimal.valueOf(3.75 + distancia * 1.45 + adicionalRegiao)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularTaxaCliente(Restaurante restaurante, Endereco destino) {
        double distancia = calcularDistancia(destino, restaurante.getEndereco());
        return BigDecimal.valueOf(3.90 + distancia * 1.10)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularTaxaPedido(Pedido pedido) {
        return BigDecimal.valueOf(5.00 + calcularDistanciaPedido(pedido) * 1.25)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calcularAdicionalGeograficoDosItens(Pedido pedido) {
        return pedido.getItens().stream()
                .map(item -> item.calcularParcelaGeografica(pedido.getEnderecoEntrega()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public double calcularDistancia(Restaurante restaurante, Endereco destino) {
        return calcularDistancia(restaurante.getEndereco(), destino);
    }

    public double calcularDistancia(Endereco origem, Endereco destino) {
        double latitudeKm = (origem.getLocalizacao().getLatitude() - destino.getLocalizacao().getLatitude()) * 110.57;
        double longitudeKm = (origem.getLocalizacao().getLongitude() - destino.getLocalizacao().getLongitude()) * 96.48;
        return Math.sqrt(latitudeKm * latitudeKm + longitudeKm * longitudeKm);
    }

    public double calcularDistanciaPedido(Pedido pedido) {
        double diferencaLatitude = pedido.getRestaurante().buscarLatitude() - pedido.getEnderecoEntrega().getLocalizacao().getLatitude();
        double diferencaLongitude = pedido.getRestaurante().buscarLongitude() - pedido.getEnderecoEntrega().getLocalizacao().getLongitude();
        return Math.sqrt(diferencaLatitude * diferencaLatitude + diferencaLongitude * diferencaLongitude) * 111.0;
    }
}
