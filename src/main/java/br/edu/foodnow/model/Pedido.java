package br.edu.foodnow.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
public class Pedido {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private Cliente cliente;
    @ManyToOne(optional = false)
    private Restaurante restaurante;
    @ManyToOne(optional = false)
    private Endereco enderecoEntrega;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal taxaEntrega = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    private double distanciaEntregaKm;
    @Enumerated(EnumType.STRING)
    private StatusPedido status = StatusPedido.CRIADO;

    protected Pedido() {
    }

    public Pedido(Cliente cliente, Restaurante restaurante, Endereco enderecoEntrega) {
        this.cliente = cliente;
        this.restaurante = restaurante;
        this.enderecoEntrega = enderecoEntrega;
    }

    public void adicionarItem(Produto produto, int quantidade) {
        boolean mesmoRestaurante = produto.getRestaurante() == restaurante
                || produto.getRestaurante().getId() != null
                && Objects.equals(produto.getRestaurante().getId(), restaurante.getId());
        if (!mesmoRestaurante) {
            throw new IllegalArgumentException("Produto pertence a outro restaurante");
        }
        itens.add(new ItemPedido(produto, quantidade));
        recalcularValores();
    }

    public void definirEntrega(double distanciaEntregaKm, BigDecimal taxaEntrega) {
        this.distanciaEntregaKm = distanciaEntregaKm;
        this.taxaEntrega = taxaEntrega;
        recalcularValores();
    }

    private void recalcularValores() {
        subtotal = itens.stream().map(ItemPedido::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        valorTotal = subtotal.add(taxaEntrega).setScale(2, RoundingMode.HALF_UP);
    }

    public double calcularDistanciaEntrega() {
        double diferencaLatitude = restaurante.buscarLatitude() - enderecoEntrega.getLocalizacao().getLatitude();
        double diferencaLongitude = restaurante.buscarLongitude() - enderecoEntrega.getLocalizacao().getLongitude();
        return Math.sqrt(diferencaLatitude * diferencaLatitude + diferencaLongitude * diferencaLongitude) * 111.0;
    }

    public BigDecimal calcularTaxaEntregaPorDistancia() {
        return BigDecimal.valueOf(5.00 + calcularDistanciaEntrega() * 1.25)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public boolean verificarEnderecoAtendido() {
        return calcularDistanciaEntrega() <= restaurante.getRaioEntregaKm();
    }

    public void confirmar() {
        if (itens.isEmpty()) {
            throw new IllegalStateException("Pedido deve possuir ao menos um item");
        }
        if (status != StatusPedido.CRIADO) {
            throw new IllegalStateException("Apenas pedidos criados podem ser confirmados");
        }
        status = StatusPedido.CONFIRMADO;
    }

    public void registrarPagamento(StatusPagamento statusPagamento) {
        if (status != StatusPedido.CONFIRMADO) {
            throw new IllegalStateException("Pedido precisa estar confirmado para pagamento");
        }
        status = statusPagamento == StatusPagamento.APROVADO
                ? StatusPedido.PAGO : StatusPedido.PAGAMENTO_REJEITADO;
    }

    public void iniciarEntrega() { status = StatusPedido.EM_ENTREGA; }
    public void concluirEntrega() { status = StatusPedido.ENTREGUE; }
    public Long getId() { return id; }
    public Cliente getCliente() { return cliente; }
    public Restaurante getRestaurante() { return restaurante; }
    public Endereco getEnderecoEntrega() { return enderecoEntrega; }
    public List<ItemPedido> getItens() { return Collections.unmodifiableList(itens); }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxaEntrega() { return taxaEntrega; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public double getDistanciaEntregaKm() { return distanciaEntregaKm; }
    public StatusPedido getStatus() { return status; }
}
