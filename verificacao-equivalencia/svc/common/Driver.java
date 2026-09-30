import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.*;
import br.edu.foodnow.repository.*;
import br.edu.foodnow.service.*;
import java.math.BigDecimal;
import java.util.*;

public class Driver {
    public record Svc(PedidoService pedidos, PagamentoService pagamentos, EntregaService entregas, FakeEmailClient email) {}

    public static void main(String[] args) throws Exception {
        int n = Integer.parseInt(args[0]);
        ClienteRepository cr = Repos.repo(ClienteRepository.class);
        RestauranteRepository rr = Repos.repo(RestauranteRepository.class);
        ProdutoRepository pr = Repos.repo(ProdutoRepository.class);
        PedidoRepository ped = Repos.repo(PedidoRepository.class);
        PagamentoRepository pagr = Repos.repo(PagamentoRepository.class);
        EntregaRepository er = Repos.repo(EntregaRepository.class);
        Svc svc = Wiring.build(cr, rr, pr, ped, pagr, er);

        for (Harness.Scen s : Harness.scenarios(n)) {
            svc.email().limpar();
            StringBuilder out = new StringBuilder();
            Restaurante r = new Restaurante("R", s.raio(), Harness.end(s.restBairro(), s.restCidade(), s.restCep(), s.restLat(), s.restLon()));
            Cliente c = new Cliente("C", "c" + s.id() + "@x");
            c.adicionarEndereco(Harness.end(s.priBairro(), s.cliCidade(), s.cliCep(), s.priLat(), s.priLon()));
            Endereco entrega = Harness.end(s.cliBairro(), s.cliCidade(), s.cliCep(), s.cliLat(), s.cliLon());
            c.adicionarEndereco(entrega);
            rr.save(r); cr.save(c);
            List<PedidoService.ItemSolicitado> itens = new ArrayList<>();
            for (int k = 0; k < s.nItens(); k++) {
                Produto p = pr.save(new Produto("P" + k, BigDecimal.valueOf(s.precos()[k]), s.id() % 7 != 0 || k > 0, r));
                itens.add(new PedidoService.ItemSolicitado(p.getId(), s.qtds()[k]));
            }
            try {
                Pedido p = svc.pedidos().criar(c.getId(), r.getId(), entrega.getId(), itens);
                out.append(String.join(";", "sub=" + p.getSubtotal(), "taxa=" + p.getTaxaEntrega(), "total=" + p.getValorTotal(),
                        "dist=" + p.getDistanciaEntregaKm(), "st=" + p.getStatus()));
                if (s.id() % 5 == 0) svc.pedidos().adicionarItem(p.getId(), itens.get(0).produtoId(), 1);
                out.append(" | conf=" + svc.pedidos().confirmar(p.getId()).getStatus());
                try { svc.pedidos().confirmar(p.getId()); } catch (RegraNegocioException e) { out.append(" | dupConf=" + e.getMessage()); }
                Pagamento pg = svc.pedidos().pagar(p.getId(), FormaPagamento.PIX, s.id() % 3 == 0 ? "REJEITADO" : "OK");
                out.append(" | pag=" + String.join(",", "" + pg.getStatus(), pg.getRegiaoEntrega(), "" + pg.getDistanciaValidadaKm(),
                        "" + pg.possuiRiscoGeografico(), pg.getCodigoExterno(), pg.getMensagemProvedor(), "" + pg.getValor()));
                out.append(" | pedSt=" + svc.pedidos().consultar(p.getId()).getStatus());
                try {
                    Entrega e = svc.entregas().consultarPorPedido(p.getId());
                    out.append(" | ent=" + String.join(",", "" + e.getDistanciaKm(), "" + e.getTempoEstimadoMinutos(), e.getZonaEntrega(),
                            "" + e.getCodigoEntregadorExterno(), e.getStatusDespachoExterno(), "" + e.getStatus(), "" + e.calcularCustoOperacional()));
                    svc.entregas().atualizarStatus(e.getId(), StatusEntrega.EM_ROTA);
                    svc.entregas().atualizarStatus(e.getId(), StatusEntrega.ENTREGUE);
                    out.append(" | finalPed=" + svc.pedidos().consultar(p.getId()).getStatus());
                } catch (RecursoNaoEncontradoException e) { out.append(" | semEntrega=" + e.getMessage()); }
                try { svc.pedidos().pagar(p.getId(), FormaPagamento.PIX, "OK"); } catch (RegraNegocioException e) { out.append(" | repag=" + e.getMessage()); }
            } catch (RegraNegocioException | RecursoNaoEncontradoException e) {
                out.append("ERRO " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
            for (Notificacao no : svc.email().getEnviadas()) out.append("\n     @ " + no.getDestinatario() + " | " + no.getAssunto() + " | " + no.getMensagem());
            System.out.println(s.id() + " " + out);
        }
    }
}
