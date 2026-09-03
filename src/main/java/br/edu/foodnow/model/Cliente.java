package br.edu.foodnow.model;

import br.edu.foodnow.util.DistanciaUtil;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
public class Cliente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String email;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<Endereco> enderecos = new ArrayList<>();

    protected Cliente() {
    }

    public Cliente(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    public void adicionarEndereco(Endereco endereco) {
        if (enderecos.isEmpty()) {
            endereco.setPrincipal(true);
        }
        enderecos.add(endereco);
    }

    public void definirEnderecoPrincipal(Long enderecoId) {
        boolean encontrado = false;
        for (Endereco endereco : enderecos) {
            boolean principal = Objects.equals(endereco.getId(), enderecoId);
            endereco.setPrincipal(principal);
            encontrado = encontrado || principal;
        }
        if (!encontrado) {
            throw new IllegalArgumentException("Endereço não pertence ao cliente");
        }
    }

    public Endereco enderecoPrincipal() {
        return enderecos.stream().filter(Endereco::isPrincipal).findFirst()
                .orElseThrow(() -> new IllegalStateException("Cliente sem endereço principal"));
    }

    public double calcularDistanciaAte(Restaurante restaurante) {
        return DistanciaUtil.calcularKm(enderecoPrincipal().getLocalizacao(),
                restaurante.getEndereco().getLocalizacao());
    }

    public boolean estaDentroDaAreaDeEntrega(Restaurante restaurante) {
        return calcularDistanciaAte(restaurante) <= restaurante.getRaioEntregaKm();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public List<Endereco> getEnderecos() { return Collections.unmodifiableList(enderecos); }
}
