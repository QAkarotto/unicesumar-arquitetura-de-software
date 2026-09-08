package br.edu.foodnow.model;

import br.edu.foodnow.util.DistanciaUtil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
public class Restaurante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private double raioEntregaKm;
    @OneToOne(cascade = CascadeType.ALL)
    private Endereco endereco;

    protected Restaurante() {
    }

    public Restaurante(String nome, double raioEntregaKm, Endereco endereco) {
        this.nome = nome;
        this.raioEntregaKm = raioEntregaKm;
        this.endereco = endereco;
    }

    public double calcularDistanciaAte(Endereco destino) {
        return DistanciaUtil.calcularKm(endereco.getLocalizacao(), destino.getLocalizacao());
    }

    public BigDecimal calcularTaxaEntrega(Endereco destino) {
        double distancia = calcularDistanciaAte(destino);
        return BigDecimal.valueOf(4.00 + distancia * 1.35).setScale(2, RoundingMode.HALF_UP);
    }

    public double buscarLatitude() { return endereco.getLocalizacao().getLatitude(); }
    public double buscarLongitude() { return endereco.getLocalizacao().getLongitude(); }
    public boolean atendeEndereco(Endereco destino) {
        return calcularDistanciaAte(destino) <= raioEntregaKm
                && endereco.pertenceARegiaoDo(destino);
    }

    public String classificarRegiaoDeEntrega(Endereco destino) {
        double distancia = calcularDistanciaAte(destino);
        if (!endereco.pertenceARegiaoDo(destino)) {
            return "FORA_DA_REGIAO";
        }
        return distancia <= raioEntregaKm / 2 ? "PROXIMA" : "LIMITE";
    }

    public int estimarTempoEntrega(Endereco destino) {
        return 12 + (int) Math.ceil(endereco.calcularDistanciaAte(destino) * 3.8);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public double getRaioEntregaKm() { return raioEntregaKm; }
    public Endereco getEndereco() { return endereco; }
}
