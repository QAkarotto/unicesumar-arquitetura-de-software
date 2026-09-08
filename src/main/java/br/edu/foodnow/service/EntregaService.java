package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.integration.FakeCourierClient;
import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Entrega;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.StatusEntrega;
import br.edu.foodnow.repository.EntregaRepository;
import br.edu.foodnow.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaService {
    private final EntregaRepository entregaRepository;
    private final PedidoRepository pedidoRepository;
    private final FakeMapsClient mapsClient;
    private final FakeEmailClient emailClient;
    private final FakeCourierClient courierClient;

    public EntregaService(EntregaRepository entregaRepository, PedidoRepository pedidoRepository,
                          FakeMapsClient mapsClient, FakeEmailClient emailClient,
                          FakeCourierClient courierClient) {
        this.entregaRepository = entregaRepository;
        this.pedidoRepository = pedidoRepository;
        this.mapsClient = mapsClient;
        this.emailClient = emailClient;
        this.courierClient = courierClient;
    }

    public Entrega criarPara(Pedido pedido) {
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(
                pedido.getRestaurante().getEndereco().getLocalizacao(),
                pedido.getEnderecoEntrega().getLocalizacao());
        double distanciaEndereco = pedido.getRestaurante().getEndereco()
                .calcularDistanciaAte(pedido.getEnderecoEntrega());
        double distanciaPedido = pedido.calcularDistanciaEntrega();
        double distanciaEscolhida = Math.max(rota.distanceKm(), Math.max(distanciaEndereco, distanciaPedido));
        String zona = "EXPANDIDA".equals(rota.deliveryZone())
                ? rota.deliveryZone() : pedido.getEnderecoEntrega().classificarZonaDeEntrega();
        FakeCourierClient.CourierRequest requisicao = new FakeCourierClient.CourierRequest(pedido.getId(),
                distanciaEscolhida, zona, pedido.getEnderecoEntrega().getLocalizacao().formatarParaProvedor());
        FakeCourierClient.CourierDispatch despacho = courierClient.solicitarEntregador(requisicao);
        int tempo = Math.max(rota.durationMinutes(), pedido.estimarTempoEntregaPeloPedido())
                + despacho.pickupEtaMinutes();
        Entrega entrega = new Entrega(pedido, pedido.getEnderecoEntrega(), distanciaEscolhida, tempo,
                zona, despacho.courierCode(), despacho.providerStatus());
        return entregaRepository.save(entrega);
    }

    public Entrega consultar(Long id) {
        return entregaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada"));
    }

    public Entrega consultarPorPedido(Long pedidoId) {
        return entregaRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Entrega não encontrada para o pedido"));
    }

    @Transactional
    public Entrega atualizarStatus(Long id, StatusEntrega novoStatus) {
        Entrega entrega = consultar(id);
        Pedido pedido = entrega.getPedido();
        entrega.atualizarStatus(novoStatus);
        if (novoStatus == StatusEntrega.EM_ROTA) {
            pedido.iniciarEntrega();
            emailClient.enviar(pedido.getCliente().getEmail(), "Entrega iniciada",
                    "O pedido " + pedido.getId() + " saiu para entrega.");
        } else if (novoStatus == StatusEntrega.ENTREGUE) {
            pedido.concluirEntrega();
        }
        pedidoRepository.save(pedido);
        return entregaRepository.save(entrega);
    }
}
