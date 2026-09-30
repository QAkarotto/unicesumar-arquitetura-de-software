import br.edu.foodnow.integration.*;
import br.edu.foodnow.model.*;
import br.edu.foodnow.util.TaxaEntregaUtil;
import java.math.BigDecimal;

/** Cópia fiel da aritmética original (PedidoService.criar, confirmar, PagamentoService, NotificacaoService, EntregaService.criarPara). */
public class Flow {
    static final FakeMapsClient mapsClient = new FakeMapsClient();
    static final FakeCourierClient courierClient = new FakeCourierClient();

    static BigDecimal maiorTaxa(BigDecimal... taxas) {
        BigDecimal maior = BigDecimal.ZERO;
        for (BigDecimal taxa : taxas) if (taxa.compareTo(maior) > 0) maior = taxa;
        return maior;
    }

    public static String run(Harness.Scen s) {
        Restaurante restaurante = new Restaurante("R", s.raio(), Harness.end(s.restBairro(), s.restCidade(), s.restCep(), s.restLat(), s.restLon()));
        Cliente cliente = new Cliente("C", "c@x");
        cliente.adicionarEndereco(Harness.end(s.priBairro(), s.cliCidade(), s.cliCep(), s.priLat(), s.priLon()));
        Endereco endereco = Harness.end(s.cliBairro(), s.cliCidade(), s.cliCep(), s.cliLat(), s.cliLon());
        cliente.adicionarEndereco(endereco);

        // --- PedidoService.criar (original)
        double distanciaDoService = mapsClient.calcularRota(restaurante.getEndereco().getLocalizacao(), endereco.getLocalizacao()).distanceKm();
        FakeMapsClient.RouteResult rotaConcreta = mapsClient.calcularRota(
                restaurante.getEndereco().getLocalizacao(), endereco.getLocalizacao());
        double distanciaDoRestaurante = restaurante.calcularDistanciaAte(endereco);
        double distanciaDoEndereco = restaurante.getEndereco().calcularDistanciaAte(endereco);
        double distanciaManhattan = restaurante.getEndereco().getLocalizacao()
                .calcularDistanciaManhattan(endereco.getLocalizacao());
        double distancia = Math.max(distanciaDoService, Math.max(rotaConcreta.distanceKm(),
                Math.max(distanciaDoRestaurante, Math.max(distanciaDoEndereco, distanciaManhattan))));
        if (distancia > restaurante.getRaioEntregaKm() || !cliente.estaDentroDaAreaDeEntrega(restaurante)
                || !restaurante.atendeEndereco(endereco)) {
            return "FORA distancia=" + distancia;
        }
        Pedido pedido = new Pedido(cliente, restaurante, endereco);
        BigDecimal taxaEntrega = maiorTaxa(TaxaEntregaUtil.calcular(distancia),
                restaurante.calcularTaxaEntrega(endereco), restaurante.getEndereco().calcularTaxaLocalAte(endereco),
                cliente.calcularTaxaEntregaDoRestaurante(restaurante), pedido.calcularTaxaEntregaPorDistancia());
        pedido.definirEntrega(distancia, taxaEntrega);
        for (int k = 0; k < s.nItens(); k++) {
            Produto p = new Produto("P" + k, BigDecimal.valueOf(s.precos()[k]), true, restaurante);
            if (!p.podeSerEntregueEm(endereco)) return "PRODUTO_NAO_ENTREGA";
            pedido.adicionarItem(p, s.qtds()[k]);
        }
        pedido.definirEntrega(distancia, taxaEntrega.add(pedido.calcularAdicionalGeograficoDosItens()));

        // --- confirmar: mensagem
        String msgConfirmar = "O pedido " + pedido.getId() + " foi confirmado para a região "
                + pedido.determinarRegiaoEntrega() + ". Estimativa interna: "
                + pedido.estimarTempoEntregaPeloPedido() + " minutos.";

        // --- PagamentoService (parte geográfica)
        FakeMapsClient.RouteResult rotaPagamento = mapsClient.calcularRota(
                pedido.getRestaurante().getEndereco().getLocalizacao(), pedido.getEnderecoEntrega().getLocalizacao());
        String regiao = pedido.determinarRegiaoEntrega();

        // --- NotificacaoService (aprovado / rejeitado)
        FakeMapsClient.RouteResult rotaNot = mapsClient.calcularRota(
                pedido.getRestaurante().getEndereco().getLocalizacao(), pedido.getEnderecoEntrega().getLocalizacao());
        Notificacao aprovado = new Notificacao("c@x", "Pagamento aprovado",
                "O pagamento do pedido " + pedido.getId() + " foi aprovado. Rota estimada pelo e-mail: "
                        + rotaNot.durationMinutes() + " minutos.").adicionarReferenciaGeografica(pedido.getEnderecoEntrega());
        Notificacao rejeitado = new Notificacao("c@x", "Pagamento rejeitado",
                "O pagamento do pedido " + pedido.getId() + " foi rejeitado na zona "
                        + rotaNot.deliveryZone() + ".").adicionarReferenciaGeografica(pedido.getEnderecoEntrega());

        // --- EntregaService.criarPara (original)
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(
                pedido.getRestaurante().getEndereco().getLocalizacao(), pedido.getEnderecoEntrega().getLocalizacao());
        double distanciaEndereco = pedido.getRestaurante().getEndereco().calcularDistanciaAte(pedido.getEnderecoEntrega());
        double distanciaPedido = pedido.calcularDistanciaEntrega();
        double distanciaEscolhida = Math.max(rota.distanceKm(), Math.max(distanciaEndereco, distanciaPedido));
        String zona = "EXPANDIDA".equals(rota.deliveryZone()) ? rota.deliveryZone() : pedido.getEnderecoEntrega().classificarZonaDeEntrega();
        FakeCourierClient.CourierRequest req = new FakeCourierClient.CourierRequest(pedido.getId(), distanciaEscolhida, zona,
                pedido.getEnderecoEntrega().getLocalizacao().formatarParaProvedor());
        FakeCourierClient.CourierDispatch desp = courierClient.solicitarEntregador(req);
        int tempo = Math.max(rota.durationMinutes(), pedido.estimarTempoEntregaPeloPedido()) + desp.pickupEtaMinutes();
        Entrega entrega = new Entrega(pedido, pedido.getEnderecoEntrega(), distanciaEscolhida, tempo, zona, desp.courierCode(), desp.providerStatus());

        return String.join(" ; ",
                "dist=" + distancia, "taxaBase=" + taxaEntrega, "taxaFinal=" + pedido.getTaxaEntrega(),
                "sub=" + pedido.getSubtotal(), "total=" + pedido.getValorTotal(), "distPedido=" + pedido.getDistanciaEntregaKm(),
                "msgConf=" + msgConfirmar, "regiao=" + regiao, "pagDist=" + rotaPagamento.distanceKm(),
                "aprov=" + aprovado.getMensagem(), "rej=" + rejeitado.getMensagem(),
                "entDist=" + entrega.getDistanciaKm(), "entTempo=" + entrega.getTempoEstimadoMinutos(), "entZona=" + entrega.getZonaEntrega(),
                "courier=" + entrega.getCodigoEntregadorExterno() + "/" + entrega.getStatusDespachoExterno(),
                "custoOp=" + entrega.calcularCustoOperacional());
    }
}
