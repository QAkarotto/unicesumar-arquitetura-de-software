package br.edu.foodnow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import java.math.BigDecimal;

@Entity
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    private Pedido pedido;
    @Enumerated(EnumType.STRING)
    private FormaPagamento forma;
    @Enumerated(EnumType.STRING)
    private StatusPagamento status;
    private BigDecimal valor;
    private String codigoExterno;
    private String mensagemProvedor;

    protected Pagamento() {
    }

    public Pagamento(Pedido pedido, FormaPagamento forma, StatusPagamento status, BigDecimal valor,
                     String codigoExterno, String mensagemProvedor) {
        this.pedido = pedido;
        this.forma = forma;
        this.status = status;
        this.valor = valor;
        this.codigoExterno = codigoExterno;
        this.mensagemProvedor = mensagemProvedor;
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public FormaPagamento getForma() { return forma; }
    public StatusPagamento getStatus() { return status; }
    public BigDecimal getValor() { return valor; }
    public String getCodigoExterno() { return codigoExterno; }
    public String getMensagemProvedor() { return mensagemProvedor; }
}
