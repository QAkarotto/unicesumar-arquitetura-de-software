package br.edu.foodnow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

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
    public Long getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getPreco() { return preco; }
    public boolean isDisponivel() { return disponivel; }
    public Restaurante getRestaurante() { return restaurante; }
}
