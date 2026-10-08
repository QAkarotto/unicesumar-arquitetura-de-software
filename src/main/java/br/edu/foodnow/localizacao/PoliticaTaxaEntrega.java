package br.edu.foodnow.localizacao;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.util.TaxaEntregaUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Dono único da formação da taxa de entrega.
 *
 * <p>O FoodNow tem cinco fórmulas de frete convivendo, uma no utilitário de faixas e quatro
 * espalhadas por restaurante, endereço, cliente e pedido, cada uma com base e multiplicador
 * próprios. A regra vigente é cobrar a maior delas e depois somar o adicional geográfico dos
 * itens. Essa reconciliação era uma chamada anônima a {@code maiorTaxa} dentro do caso de uso e
 * agora é uma decisão nomeada, com um lugar só para mudar.</p>
 *
 * <p>Quando a taxa passar a considerar trajeto, região e horário, é aqui que a nova política
 * entra, recebendo a {@link RotaEntrega} já resolvida.</p>
 */
@Component
public class PoliticaTaxaEntrega {

    public BigDecimal calcularBase(Pedido pedido, double distanciaKm) {
        Cliente cliente = pedido.getCliente();
        Restaurante restaurante = pedido.getRestaurante();
        Endereco destino = pedido.getEnderecoEntrega();
        return maior(
                TaxaEntregaUtil.calcular(distanciaKm),
                restaurante.calcularTaxaEntrega(destino),
                restaurante.getEndereco().calcularTaxaLocalAte(destino),
                cliente.calcularTaxaEntregaDoRestaurante(restaurante),
                pedido.calcularTaxaEntregaPorDistancia());
    }

    public BigDecimal calcularComAdicionalDosItens(Pedido pedido, double distanciaKm) {
        return calcularBase(pedido, distanciaKm).add(pedido.calcularAdicionalGeograficoDosItens());
    }

    private static BigDecimal maior(BigDecimal... taxas) {
        BigDecimal maior = BigDecimal.ZERO;
        for (BigDecimal taxa : taxas) {
            if (taxa.compareTo(maior) > 0) {
                maior = taxa;
            }
        }
        return maior;
    }
}
