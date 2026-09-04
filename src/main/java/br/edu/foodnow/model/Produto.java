package br.edu.foodnow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private BigDecimal preco;
    private boolean disponivel;
    @ManyToOne(optional = false)
    private Restaurante restaurante;

    protected Produto() {
    }

    public Produto(String nome, BigDecimal preco, boolean disponivel, Restaurante restaurante) {
        this.nome = nome;
        this.preco = preco;
        this.disponivel = disponivel;
        this.restaurante = restaurante;
    }

    public void alterarDisponibilidade(boolean disponivel) { this.disponivel = disponivel; }

    public boolean podeSerEntregueEm(Endereco destino) {
        return disponivel && restaurante.atendeEndereco(destino);
    }

    public BigDecimal calcularAdicionalRegional(Endereco destino) {
        double distancia = restaurante.getEndereco().calcularDistanciaAte(destino);
        double adicional = "CENTRAL".equals(destino.classificarZonaDeEntrega()) ? 0.0 : distancia * 0.08;
        return BigDecimal.valueOf(adicional).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getPreco() { return preco; }
    public boolean isDisponivel() { return disponivel; }
    public Restaurante getRestaurante() { return restaurante; }
}
