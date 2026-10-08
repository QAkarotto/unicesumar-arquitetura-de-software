package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeEmailClient;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.repository.ClienteRepository;
import br.edu.foodnow.repository.PedidoRepository;
import br.edu.foodnow.repository.ProdutoRepository;
import br.edu.foodnow.repository.RestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final PoliticaEntregaService politicaEntregaService;
    private final PagamentoService pagamentoService;
    private final FakeEmailClient emailClient;

    public PedidoService(ClienteRepository clienteRepository, RestauranteRepository restauranteRepository,
                         ProdutoRepository produtoRepository, PedidoRepository pedidoRepository,
                         PoliticaEntregaService politicaEntregaService, PagamentoService pagamentoService,
                         FakeEmailClient emailClient) {
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.politicaEntregaService = politicaEntregaService;
        this.pagamentoService = pagamentoService;
        this.emailClient = emailClient;
    }

    @Transactional
    public Pedido criar(Long clienteId, Long restauranteId, Long enderecoEntregaId,
                        List<ItemSolicitado> itensSolicitados) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Restaurante não encontrado"));
        Endereco endereco = cliente.getEnderecos().stream()
                .filter(item -> item.getId().equals(enderecoEntregaId))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Endereço não pertence ao cliente"));

        PoliticaEntregaService.AvaliacaoAreaEntrega avaliacao = politicaEntregaService
                .avaliarParaNovoPedido(cliente, restaurante, endereco);
        if (!avaliacao.atendido()) {
            throw new RegraNegocioException("Endereço fora da área de entrega");
        }

        Pedido pedido = new Pedido(cliente, restaurante, endereco);
        BigDecimal taxaEntrega = politicaEntregaService.calcularTaxaEntrega(cliente, restaurante, endereco, pedido,
                avaliacao.distanciaKm());
        pedido.definirEntrega(avaliacao.distanciaKm(), taxaEntrega);
        for (ItemSolicitado item : itensSolicitados) {
            adicionarItemCarregado(pedido, item.produtoId(), item.quantidade());
        }
        pedido.definirEntrega(avaliacao.distanciaKm(), taxaEntrega.add(pedido.calcularAdicionalGeograficoDosItens()));
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido adicionarItem(Long pedidoId, Long produtoId, int quantidade) {
        Pedido pedido = consultar(pedidoId);
        adicionarItemCarregado(pedido, produtoId, quantidade);
        return pedidoRepository.save(pedido);
    }

    private void adicionarItemCarregado(Pedido pedido, Long produtoId, int quantidade) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
        if (!produto.isDisponivel()) {
            throw new RegraNegocioException("Produto indisponível: " + produto.getNome());
        }
        if (!produto.podeSerEntregueEm(pedido.getEnderecoEntrega())) {
            throw new RegraNegocioException("Produto não pode ser entregue no endereço selecionado");
        }
        if (!produto.getRestaurante().getId().equals(pedido.getRestaurante().getId())) {
            throw new RegraNegocioException("Produto pertence a outro restaurante");
        }
        try {
            pedido.adicionarItem(produto, quantidade);
        } catch (IllegalArgumentException excecao) {
            throw new RegraNegocioException(excecao.getMessage());
        }
    }

    public Pedido consultar(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado"));
    }

    @Transactional
    public Pedido confirmar(Long id) {
        Pedido pedido = consultar(id);
        try {
            pedido.confirmar();
        } catch (IllegalStateException excecao) {
            throw new RegraNegocioException(excecao.getMessage());
        }
        emailClient.enviar(pedido.getCliente().getEmail(), "Pedido confirmado",
                "O pedido " + pedido.getId() + " foi confirmado para a região "
                        + pedido.determinarRegiaoEntrega() + ". Estimativa interna: "
                        + pedido.estimarTempoEntregaPeloPedido() + " minutos.");
        return pedidoRepository.save(pedido);
    }

    public Pagamento pagar(Long pedidoId, FormaPagamento forma, String token) {
        return pagamentoService.processar(pedidoId, forma, token);
    }

    public record ItemSolicitado(Long produtoId, int quantidade) {
    }
}
