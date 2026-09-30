package br.edu.foodnow.entrega;

import br.edu.foodnow.model.Pedido;

import java.math.BigDecimal;

/**
 * Limite arquitetural das regras de distância, região, atendimento, taxa e tempo de entrega.
 * <p>
 * Pedido, pagamento, notificação e entrega consultam esta interface em vez de cada um
 * calcular (ou escolher entre) as suas próprias medidas. A futura regra de taxa por rota,
 * região e horário deve ser uma nova implementação desta interface (ou uma evolução de
 * {@link PoliticaEntregaPadrao}); os services não precisam mudar.
 */
public interface PoliticaEntrega {

    /** Decide distância efetiva, atendimento e taxa base para um pedido a ser criado. */
    CotacaoEntrega cotar(SolicitacaoCotacao solicitacao);

    /** Taxa final = taxa base da cotação + adicional geográfico dos itens do pedido. */
    BigDecimal taxaFinal(CotacaoEntrega cotacao, Pedido pedido);

    /** Rota e região de um pedido existente. */
    AvaliacaoGeografica avaliar(Pedido pedido);

    /** Região de entrega do pedido (CENTRAL, URBANA, EXTERNA ou NAO_ATENDIDA). */
    String regiaoDeEntrega(Pedido pedido);

    /** Estimativa de tempo usada na confirmação do pedido, em minutos. */
    int estimarTempoInterno(Pedido pedido);

    /** Distância, zona e tempo-base do despacho de um pedido pago. */
    PlanoEntrega planejarEntrega(Pedido pedido);
}
