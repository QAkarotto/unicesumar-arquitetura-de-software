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
    @Enumerated(EnumType.STRING)
    private StatusEntrega status = StatusEntrega.AGUARDANDO_ENTREGADOR;

    protected Entrega() {
    }

    public Entrega(Pedido pedido, Endereco endereco, double distanciaKm, int tempoEstimadoMinutos) {
        this.pedido = pedido;
        this.endereco = endereco;
        this.distanciaKm = distanciaKm;
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
    }

    public double calcularDistancia() {
        return DistanciaUtil.calcularKm(pedido.getRestaurante().getEndereco().getLocalizacao(),
                endereco.getLocalizacao());
    }

    public int estimarTempoEntrega() {
        return 15 + (int) Math.ceil(calcularDistancia() * 3.2);
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
}
