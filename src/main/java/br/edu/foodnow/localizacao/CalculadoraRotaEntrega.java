package br.edu.foodnow.localizacao;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import org.springframework.stereotype.Component;

/**
 * Dono único do cálculo de distância entre o restaurante e o endereço de entrega.
 *
 * <p>O FoodNow mede essa distância por aproximações diferentes que não convergem, trajeto do
 * provedor, Haversine, projeção cartesiana em graus e Manhattan. A regra vigente é adotar a maior
 * delas, e essa regra passa a morar aqui em vez de ficar diluída em um {@code Math.max} aninhado
 * dentro do caso de uso.</p>
 *
 * <p>É este componente que a futura regra de trajeto, região e horário deve alterar. Nenhum
 * consumidor precisa saber quantas fórmulas existem nem qual delas venceu.</p>
 */
@Component
public class CalculadoraRotaEntrega {
    private static final String REGIAO_EXPANDIDA = "EXPANDIDA";

    private final MapaGateway mapaGateway;

    public CalculadoraRotaEntrega(MapaGateway mapaGateway) {
        this.mapaGateway = mapaGateway;
    }

    public RotaEntrega paraCriacaoDePedido(Restaurante restaurante, Endereco destino) {
        Endereco origem = restaurante.getEndereco();
        Rota rotaProvedor = mapaGateway.calcularRota(origem.getLocalizacao(), destino.getLocalizacao());
        double distanciaReconciliada = maior(
                rotaProvedor.distanciaKm(),
                restaurante.calcularDistanciaAte(destino),
                origem.calcularDistanciaAte(destino),
                origem.getLocalizacao().calcularDistanciaManhattan(destino.getLocalizacao()));
        return new RotaEntrega(distanciaReconciliada, rotaProvedor);
    }

    public RotaEntrega paraDespachoDeEntrega(Pedido pedido) {
        Endereco origem = pedido.getRestaurante().getEndereco();
        Endereco destino = pedido.getEnderecoEntrega();
        Rota rotaProvedor = mapaGateway.calcularRota(origem.getLocalizacao(), destino.getLocalizacao());
        double distanciaReconciliada = maior(
                rotaProvedor.distanciaKm(),
                origem.calcularDistanciaAte(destino),
                pedido.calcularDistanciaEntrega());
        return new RotaEntrega(distanciaReconciliada, rotaProvedor);
    }

    /**
     * Reconcilia as duas definições de região que valem no despacho. O provedor só é respeitado
     * quando classifica a entrega como expandida, caso contrário vale a zona do próprio endereço.
     */
    public String regiaoDeDespacho(RotaEntrega rota, Endereco destino) {
        return REGIAO_EXPANDIDA.equals(rota.regiaoProvedor())
                ? rota.regiaoProvedor()
                : destino.classificarZonaDeEntrega();
    }

    private static double maior(double... distancias) {
        double maior = Double.NEGATIVE_INFINITY;
        for (double distancia : distancias) {
            maior = Math.max(maior, distancia);
        }
        return maior;
    }
}
