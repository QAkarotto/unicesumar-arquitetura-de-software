package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.integration.FakeCourierClient;
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
    private final LocalizacaoService localizacaoService;
    private final FakeEmailClient emailClient;
    private final FakeCourierClient courierClient;
    private final CalculoFrete calculoFrete;
    private final ValidadorEntrega validadorEntrega;

    public EntregaService(EntregaRepository entregaRepository, PedidoRepository pedidoRepository,
                          LocalizacaoService localizacaoService, FakeEmailClient emailClient,
                          FakeCourierClient courierClient, CalculoFrete calculoFrete,
                          ValidadorEntrega validadorEntrega) {
        this.entregaRepository = entregaRepository;
        this.pedidoRepository = pedidoRepository;
        this.localizacaoService = localizacaoService;
        this.emailClient = emailClient;
        this.courierClient = courierClient;
        this.calculoFrete = calculoFrete;
        this.validadorEntrega = validadorEntrega;
    }

    public Entrega criarPara(Pedido pedido) {
        double distanciaRota = localizacaoService.calcularDistancia(
                pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        double distanciaEndereco = calculoFrete.calcularDistancia(pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        double distanciaPedido = calculoFrete.calcularDistanciaPedido(pedido);
        double distanciaEscolhida = Math.max(distanciaRota, Math.max(distanciaEndereco, distanciaPedido));
        String zona = localizacaoService.buscarZonaEntrega(
                pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        if (!"EXPANDIDA".equals(zona)) {
            zona = pedido.getEnderecoEntrega().classificarZonaDeEntrega();
        }
        FakeCourierClient.CourierRequest requisicao = new FakeCourierClient.CourierRequest(pedido.getId(),
                distanciaEscolhida, zona, pedido.getEnderecoEntrega().getLocalizacao().formatarParaProvedor());
        FakeCourierClient.CourierDispatch despacho = courierClient.solicitarEntregador(requisicao);
        int tempo = localizacaoService.estimarTempoEntrega(
                pedido.getRestaurante().getEndereco(), pedido.getEnderecoEntrega());
        tempo = Math.max(tempo, 14 + (int) Math.ceil(distanciaPedido * 3.6) + pedido.getItens().size() * 2)
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