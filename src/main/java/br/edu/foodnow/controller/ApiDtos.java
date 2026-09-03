package br.edu.foodnow.controller;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Entrega;
import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.ItemPedido;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Pagamento;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.model.StatusEntrega;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.model.StatusPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record ClienteRequest(@NotBlank String nome, @NotBlank @Email String email) {
    }

    public record EnderecoRequest(@NotBlank String logradouro, @NotBlank String numero, @NotBlank String bairro,
                                  @NotBlank String cidade, @NotBlank String cep,
                                  @NotNull Double latitude, @NotNull Double longitude) {
        public Endereco toModel() {
            return new Endereco(logradouro, numero, bairro, cidade, cep,
                    new Localizacao(latitude, longitude));
        }
    }

    public record EnderecoResponse(Long id, String logradouro, String numero, String bairro, String cidade,
                                   String cep, boolean principal, double latitude, double longitude) {
        public static EnderecoResponse from(Endereco endereco) {
            return new EnderecoResponse(endereco.getId(), endereco.getLogradouro(), endereco.getNumero(),
                    endereco.getBairro(), endereco.getCidade(), endereco.getCep(), endereco.isPrincipal(),
                    endereco.getLocalizacao().getLatitude(), endereco.getLocalizacao().getLongitude());
        }
    }

    public record ClienteResponse(Long id, String nome, String email, List<EnderecoResponse> enderecos) {
        public static ClienteResponse from(Cliente cliente) {
            return new ClienteResponse(cliente.getId(), cliente.getNome(), cliente.getEmail(),
                    cliente.getEnderecos().stream().map(EnderecoResponse::from).toList());
        }
    }

    public record RestauranteRequest(@NotBlank String nome, @DecimalMin("0.1") double raioEntregaKm,
                                     @NotNull @Valid EnderecoRequest endereco) {
    }

    public record RestauranteResponse(Long id, String nome, double raioEntregaKm, EnderecoResponse endereco) {
        public static RestauranteResponse from(Restaurante restaurante) {
            return new RestauranteResponse(restaurante.getId(), restaurante.getNome(),
                    restaurante.getRaioEntregaKm(), EnderecoResponse.from(restaurante.getEndereco()));
        }
    }

    public record ProdutoRequest(@NotBlank String nome, @NotNull @DecimalMin("0.01") BigDecimal preco,
                                 boolean disponivel) {
    }

    public record DisponibilidadeRequest(boolean disponivel) {
    }

    public record ProdutoResponse(Long id, String nome, BigDecimal preco, boolean disponivel,
                                  Long restauranteId) {
        public static ProdutoResponse from(Produto produto) {
            return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getPreco(),
                    produto.isDisponivel(), produto.getRestaurante().getId());
        }
    }

    public record ItemPedidoRequest(@NotNull Long produtoId, @Min(1) int quantidade) {
    }

    public record PedidoRequest(@NotNull Long clienteId, @NotNull Long restauranteId,
                                @NotNull Long enderecoEntregaId,
                                @NotEmpty List<@Valid ItemPedidoRequest> itens) {
    }

    public record ItemResponse(Long produtoId, String nome, int quantidade, BigDecimal precoUnitario,
                               BigDecimal subtotal) {
        public static ItemResponse from(ItemPedido item) {
            return new ItemResponse(item.getProduto().getId(), item.getProduto().getNome(), item.getQuantidade(),
                    item.getPrecoUnitario(), item.calcularSubtotal());
        }
    }

    public record PedidoResponse(Long id, Long clienteId, Long restauranteId, Long enderecoEntregaId,
                                 List<ItemResponse> itens, BigDecimal subtotal, BigDecimal taxaEntrega,
                                 BigDecimal valorTotal, double distanciaEntregaKm, StatusPedido status) {
        public static PedidoResponse from(Pedido pedido) {
            return new PedidoResponse(pedido.getId(), pedido.getCliente().getId(),
                    pedido.getRestaurante().getId(), pedido.getEnderecoEntrega().getId(),
                    pedido.getItens().stream().map(ItemResponse::from).toList(), pedido.getSubtotal(),
                    pedido.getTaxaEntrega(), pedido.getValorTotal(), pedido.getDistanciaEntregaKm(),
                    pedido.getStatus());
        }
    }

    public record PagamentoRequest(@NotNull Long pedidoId, @NotNull FormaPagamento forma,
                                   @NotBlank String token) {
    }

    public record PagamentoResponse(Long id, Long pedidoId, FormaPagamento forma, StatusPagamento status,
                                    BigDecimal valor, String codigoExterno, String mensagemProvedor) {
        public static PagamentoResponse from(Pagamento pagamento) {
            return new PagamentoResponse(pagamento.getId(), pagamento.getPedido().getId(), pagamento.getForma(),
                    pagamento.getStatus(), pagamento.getValor(), pagamento.getCodigoExterno(),
                    pagamento.getMensagemProvedor());
        }
    }

    public record EntregaResponse(Long id, Long pedidoId, Long enderecoId, double distanciaKm,
                                  int tempoEstimadoMinutos, StatusEntrega status) {
        public static EntregaResponse from(Entrega entrega) {
            return new EntregaResponse(entrega.getId(), entrega.getPedido().getId(), entrega.getEndereco().getId(),
                    entrega.getDistanciaKm(), entrega.getTempoEstimadoMinutos(), entrega.getStatus());
        }
    }

    public record AtualizarEntregaRequest(@NotNull StatusEntrega status) {
    }

    public record ErroResponse(int status, String erro, String mensagem) {
    }
}
