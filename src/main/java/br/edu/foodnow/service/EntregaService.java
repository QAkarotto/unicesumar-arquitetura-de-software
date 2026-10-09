package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeCourierClient;
import br.edu.foodnow.logistica.LogisticaService;
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
    private final LogisticaService logisticaService;
    private final NotificacaoService notificacaoService;
    private final FakeCourierClient courierClient;

    public EntregaService(EntregaRepository entregaRepository, PedidoRepository pedidoRepository,
                          LogisticaService logisticaService, NotificacaoService notificacaoService,
                          FakeCourierClient courierClient) {
        this.entregaRepository = entregaRepository;
        this.pedidoRepository = pedidoRepository;
        this.logisticaService = logisticaService;
        this.notificacaoService = notificacaoService;
        this.courierClient = courierClient;
    }

    public Entrega criarPara(Pedido pedido) {
        var plano = logisticaService.planejarEntrega(pedido);
        FakeCourierClient.CourierRequest requisicao = new FakeCourierClient.CourierRequest(pedido.getId(),
                plano.distanciaKm(), plano.zona(), pedido.getEnderecoEntrega().getLocalizacao().formatarParaProvedor());
        FakeCourierClient.CourierDispatch despacho = courierClient.solicitarEntregador(requisicao);
        int tempo = plano.tempoBaseMinutos() + despacho.pickupEtaMinutes();
        Entrega entrega = new Entrega(pedido, pedido.getEnderecoEntrega(), plano.distanciaKm(), tempo,
                plano.zona(), despacho.courierCode(), despacho.providerStatus());
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
            notificacaoService.notificarInicioEntrega(pedido);
        } else if (novoStatus == StatusEntrega.ENTREGUE) {
            pedido.concluirEntrega();
        }
        pedidoRepository.save(pedido);
        return entregaRepository.save(entrega);
    }
}
