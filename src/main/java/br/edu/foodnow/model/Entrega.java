package br.edu.foodnow.model;

import br.edu.foodnow.util.DistanciaUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
public class Entrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false)
    private Pedido pedido;
    @ManyToOne(optional = false)
    private Endereco endereco;
    private double distanciaKm;
    private int tempoEstimadoMinutos;
    private String zonaEntrega;
    private String codigoEntregadorExterno;
    private String statusDespachoExterno;
    @Enumerated(EnumType.STRING)
    private StatusEntrega status = StatusEntrega.AGUARDANDO_ENTREGADOR;

    protected Entrega() {
    }

    public Entrega(Pedido pedido, Endereco endereco, double distanciaKm, int tempoEstimadoMinutos) {
        this(pedido, endereco, distanciaKm, tempoEstimadoMinutos,
                endereco.classificarZonaDeEntrega(), null, "NOT_REQUESTED");
    }

    public Entrega(Pedido pedido, Endereco endereco, double distanciaKm, int tempoEstimadoMinutos,
                   String zonaEntrega, String codigoEntregadorExterno, String statusDespachoExterno) {
        this.pedido = pedido;
        this.endereco = endereco;
        this.distanciaKm = distanciaKm;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
        this.zonaEntrega = zonaEntrega;
        this.codigoEntregadorExterno = codigoEntregadorExterno;
        this.statusDespachoExterno = statusDespachoExterno;
    }

    public double calcularDistancia() {
        return DistanciaUtil.calcularKm(pedido.getRestaurante().getEndereco().getLocalizacao(),
                endereco.getLocalizacao());
    }

    public int estimarTempoEntrega() {
        return 15 + (int) Math.ceil(calcularDistancia() * 3.2);
    }

    public double calcularDistanciaPeloEndereco() {
        return pedido.getRestaurante().getEndereco().calcularDistanciaAte(endereco);
    }

    public BigDecimal calcularCustoOperacional() {
        double adicionalZona = "CENTRAL".equals(zonaEntrega) ? 0.0 : 2.75;
        return BigDecimal.valueOf(2.50 + calcularDistanciaPeloEndereco() * 0.95 + adicionalZona)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void atualizarStatus(StatusEntrega novoStatus) {
        this.status = novoStatus;
    }

    public Long getId() { return id; }
    public Pedido getPedido() { return pedido; }
    public Endereco getEndereco() { return endereco; }
    public double getDistanciaKm() { return distanciaKm; }
    public int getTempoEstimadoMinutos() { return tempoEstimadoMinutos; }
    public StatusEntrega getStatus() { return status; }
    public String getZonaEntrega() { return zonaEntrega; }
    public String getCodigoEntregadorExterno() { return codigoEntregadorExterno; }
    public String getStatusDespachoExterno() { return statusDespachoExterno; }
}
