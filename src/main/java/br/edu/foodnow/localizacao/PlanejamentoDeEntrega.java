package br.edu.foodnow.localizacao;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.Produto;
import org.springframework.stereotype.Component;

/**
 * Limite entre o pedido e tudo o que é geográfico.
 *
 * <p>Reúne as três decisões que antes ficavam soltas dentro de {@code PedidoService.criar}, qual é
 * a distância válida, se o endereço é atendido e quanto custa a entrega. O caso de uso passa a
 * conversar com um colaborador só, e nenhuma dessas respostas depende mais de conhecer o
 * fornecedor de mapas.</p>
 *
 * <p>A regra futura de trajeto, região e horário é acomodada por trás deste limite, alterando a
 * rota, a política de área ou a política de taxa, sem alcançar o caso de uso nem as entidades.</p>
 */
@Component
public class PlanejamentoDeEntrega {
    private final CalculadoraRotaEntrega calculadoraRotaEntrega;
    private final PoliticaAreaAtendimento politicaAreaAtendimento;
    private final PoliticaTaxaEntrega politicaTaxaEntrega;

    public PlanejamentoDeEntrega(CalculadoraRotaEntrega calculadoraRotaEntrega,
                                 PoliticaAreaAtendimento politicaAreaAtendimento,
                                 PoliticaTaxaEntrega politicaTaxaEntrega) {
        this.calculadoraRotaEntrega = calculadoraRotaEntrega;
        this.politicaAreaAtendimento = politicaAreaAtendimento;
        this.politicaTaxaEntrega = politicaTaxaEntrega;
    }

    public PlanoDeEntrega planejar(Cliente cliente, Restaurante restaurante, Endereco destino) {
        RotaEntrega rota = calculadoraRotaEntrega.paraCriacaoDePedido(restaurante, destino);
        boolean atendido = politicaAreaAtendimento.atende(cliente, restaurante, destino,
                rota.distanciaKm());
        return new PlanoDeEntrega(rota, atendido);
    }

    /** Registra no pedido a distância e a taxa base, antes de os itens entrarem. */
    public void aplicarA(Pedido pedido, PlanoDeEntrega plano) {
        pedido.definirEntrega(plano.distanciaKm(),
                politicaTaxaEntrega.calcularBase(pedido, plano.distanciaKm()));
    }

    /** Reaplica a taxa somando o adicional geográfico que os itens do pedido acrescentam. */
    public void reaplicarComItens(Pedido pedido, PlanoDeEntrega plano) {
        pedido.definirEntrega(plano.distanciaKm(),
                politicaTaxaEntrega.calcularComAdicionalDosItens(pedido, plano.distanciaKm()));
    }

    public boolean produtoAlcanca(Produto produto, Endereco destino) {
        return politicaAreaAtendimento.restauranteCobre(produto.getRestaurante(), destino);
    }
}
