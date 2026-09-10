package br.edu.foodnow.service;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import org.springframework.stereotype.Component;

@Component
public class ValidadorEntrega {

    public boolean estaDentroDaAreaDeEntrega(Restaurante restaurante, Endereco endereco) {
        return calcularDistancia(restaurante, endereco) <= restaurante.getRaioEntregaKm();
    }

    public boolean atendeEndereco(Restaurante restaurante, Endereco endereco) {
        return restaurante.atendeEndereco(endereco);
    }

    public boolean verificarEnderecoAtendido(Pedido pedido) {
        return calcularDistanciaPedido(pedido) <= pedido.getRestaurante().getRaioEntregaKm();
    }

    public String determinarRegiaoEntrega(Pedido pedido) {
        if (!pedido.getRestaurante().getEndereco().pertenceARegiaoDo(pedido.getEnderecoEntrega())) {
            return "NAO_ATENDIDA";
        }
        return pedido.getEnderecoEntrega().classificarZonaDeEntrega();
    }

    public String classificarRegiaoDeEntrega(Restaurante restaurante, Endereco destino) {
        double distancia = calcularDistancia(restaurante, destino);
        if (!restaurante.getEndereco().pertenceARegiaoDo(destino)) {
            return "FORA_DA_REGIAO";
        }
        return distancia <= restaurante.getRaioEntregaKm() / 2 ? "PROXIMA" : "LIMITE";
    }

    public double calcularDistancia(Restaurante restaurante, Endereco destino) {
        return restaurante.calcularDistanciaAte(destino);
    }

    public double calcularDistanciaPedido(Pedido pedido) {
        double diferencaLatitude = pedido.getRestaurante().buscarLatitude() - pedido.getEnderecoEntrega().getLocalizacao().getLatitude();
        double diferencaLongitude = pedido.getRestaurante().buscarLongitude() - pedido.getEnderecoEntrega().getLocalizacao().getLongitude();
        return Math.sqrt(diferencaLatitude * diferencaLatitude + diferencaLongitude * diferencaLongitude) * 111.0;
    }
}
