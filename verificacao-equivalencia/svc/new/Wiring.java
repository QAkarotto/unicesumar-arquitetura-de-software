import br.edu.foodnow.entrega.*;
import br.edu.foodnow.integration.*;
import br.edu.foodnow.repository.*;
import br.edu.foodnow.service.*;
public class Wiring {
    static Driver.Svc build(ClienteRepository cr, RestauranteRepository rr, ProdutoRepository pr, PedidoRepository ped, PagamentoRepository pagr, EntregaRepository er) {
        FakeEmailClient email = new FakeEmailClient();
        FakeCourierClient courier = new FakeCourierClient(); FakePaymentGateway gw = new FakePaymentGateway();
        PoliticaEntrega politica = new PoliticaEntregaPadrao(new MapsProvedorGeografico(new FakeMapsClient()));
        NotificacaoService not = new NotificacaoService(email, politica);
        EntregaService ent = new EntregaService(er, ped, not, courier, politica);
        PagamentoService pag = new PagamentoService(ped, pagr, gw, not, ent, politica);
        PedidoService pedidos = new PedidoService(cr, rr, pr, ped, politica, pag, not);
        return new Driver.Svc(pedidos, pag, ent, email);
    }
}
