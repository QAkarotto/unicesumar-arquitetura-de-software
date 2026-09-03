package br.edu.foodnow.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Endereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    private String cep;
    private boolean principal;
    @Embedded
    private Localizacao localizacao;

    protected Endereco() {
    }

    public Endereco(String logradouro, String numero, String bairro, String cidade, String cep,
                    Localizacao localizacao) {
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.cep = cep;
        this.localizacao = localizacao;
    }

    public String enderecoCompleto() {
        return logradouro + ", " + numero + " - " + bairro + ", " + cidade + " - " + cep;
    }

    public Long getId() { return id; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getBairro() { return bairro; }
    public String getCidade() { return cidade; }
    public String getCep() { return cep; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean principal) { this.principal = principal; }
    public Localizacao getLocalizacao() { return localizacao; }
}
