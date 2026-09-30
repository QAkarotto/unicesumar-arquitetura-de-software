import br.edu.foodnow.entrega.*;
import br.edu.foodnow.geo.*;
import br.edu.foodnow.integration.*;
import br.edu.foodnow.model.*;
import java.math.BigDecimal;

/** Mesma sequência lógica dos services refatorados, usando PoliticaEntrega + adapter. */
public class Flow {
    static final FakeCourierClient courierClient = new FakeCourierClient();
    static final PoliticaEntrega politica = new PoliticaEntregaPadrao(new MapsProvedorGeografico(new FakeMapsClient()));

    public static String run(Harness.Scen s) {
        Restaurante restaurante = new Restaurante("R", s.raio(), Harness.end(s.restBairro(), s.restCidade(), s.restCep(), s.restLat(), s.restLon()));
        Cliente cliente = new Cliente("C", "c@x");
        cliente.adicionarEndereco(Harness.end(s.priBairro(), s.cliCidade(), s.cliCep(), s.priLat(), s.priLon()));
        Endereco endereco = Harness.end(s.cliBairro(), s.cliCidade(), s.cliCep(), s.cliLat(), s.cliLon());
        cliente.adicionarEndereco(endereco);

        // --- PedidoService.criar (refatorado)
        CotacaoEntrega cotacao = politica.cotar(new SolicitacaoCotacao(cliente, restaurante, endereco));
        if (!cotacao.atendido()) return "FORA distancia=" + cotacao.distanciaKm();
        Pedido pedido = new Pedido(cliente, restaurante, endereco);
        pedido.definirEntrega(cotacao.distanciaKm(), cotacao.taxaBase());
        for (int k = 0; k < s.nItens(); k++) {
            Produto p = new Produto("P" + k, BigDecimal.valueOf(s.precos()[k]), true, restaurante);
            if (!p.podeSerEntregueEm(endereco)) return "PRODUTO_NAO_ENTREGA";
            pedido.adicionarItem(p, s.qtds()[k]);
        }
        pedido.definirEntrega(cotacao.distanciaKm(), politica.taxaFinal(cotacao, pedido));

        String msgConfirmar = "O pedido " + pedido.getId() + " foi confirmado para a região "
                + politica.regiaoDeEntrega(pedido) + ". Estimativa interna: "
                + politica.estimarTempoInterno(pedido) + " minutos.";

        AvaliacaoGeografica av = politica.avaliar(pedido);

        Notificacao aprovado = new Notificacao("c@x", "Pagamento aprovado",
                "O pagamento do pedido " + pedido.getId() + " foi aprovado. Rota estimada pelo e-mail: "
                        + av.rota().duracaoMinutos() + " minutos.").adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
        Notificacao rejeitado = new Notificacao("c@x", "Pagamento rejeitado",
                "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                        + av.rota().zona() + ".").adicionarReferenciaGeografica(pedido.getEnderecoEntrega());

        PlanoEntrega plano = politica.planejarEntrega(pedido);
        FakeCourierClient.CourierRequest req = new FakeCourierClient.CourierRequest(pedido.getId(), plano.distanciaKm(), plano.zona(),
                pedido.getEnderecoEntrega().getLocalizacao().formatarParaProvedor());
        FakeCourierClient.CourierDispatch desp = courierClient.solicitarEntregador(req);
        Entrega entrega = new Entrega(pedido, pedido.getEnderecoEntrega(), plano.distanciaKm(),
                plano.tempoTotalMinutos(desp.pickupEtaMinutes()), plano.zona(), desp.courierCode(), desp.providerStatus());

        return String.join(" ; ",
                "dist=" + cotacao.distanciaKm(), "taxaBase=" + cotacao.taxaBase(), "taxaFinal=" + pedido.getTaxaEntrega(),
                "sub=" + pedido.getSubtotal(), "total=" + pedido.getValorTotal(), "distPedido=" + pedido.getDistanciaEntregaKm(),
                "msgConf=" + msgConfirmar, "regiao=" + av.regiao(), "pagDist=" + av.rota().distanciaKm(),
                "aprov=" + aprovado.getMensagem(), "rej=" + rejeitado.getMensagem(),
                "entDist=" + entrega.getDistanciaKm(), "entTempo=" + entrega.getTempoEstimadoMinutos(), "entZona=" + entrega.getZonaEntrega(),
                "courier=" + entrega.getCodigoEntregadorExterno() + "/" + entrega.getStatusDespachoExterno(),
                "custoOp=" + entrega.calcularCustoOperacional());
    }
}
