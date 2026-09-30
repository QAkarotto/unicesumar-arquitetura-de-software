import br.edu.foodnow.integration.*;
import br.edu.foodnow.repository.*;
import br.edu.foodnow.service.*;
public class Wiring {
    static Driver.Svc build(ClienteRepository cr, RestauranteRepository rr, ProdutoRepository pr, PedidoRepository ped, PagamentoRepository pagr, EntregaRepository er) {
        FakeMapsClient maps = new FakeMapsClient(); FakeEmailClient email = new FakeEmailClient();
        FakeCourierClient courier = new FakeCourierClient(); FakePaymentGateway gw = new FakePaymentGateway();
        LocalizacaoService loc = new LocalizacaoService(maps);
        NotificacaoService not = new NotificacaoService(email, maps);
        EntregaService ent = new EntregaService(er, ped, maps, email, courier);
        PagamentoService pag = new PagamentoService(ped, pagr, gw, not, ent, maps);
        PedidoService pedidos = new PedidoService(cr, rr, pr, ped, loc, pag, email, maps);
        return new Driver.Svc(pedidos, pag, ent, email);
    }
}
