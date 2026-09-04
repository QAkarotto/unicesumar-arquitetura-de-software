package br.edu.foodnow.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public double calcularDistanciaAte(Endereco destino) {
        double latitudeKm = (localizacao.getLatitude() - destino.localizacao.getLatitude()) * 110.57;
        double longitudeKm = (localizacao.getLongitude() - destino.localizacao.getLongitude()) * 96.48;
        return Math.sqrt(latitudeKm * latitudeKm + longitudeKm * longitudeKm);
    }

    public int estimarMinutosAte(Endereco destino) {
        return 8 + (int) Math.ceil(calcularDistanciaAte(destino) * 4.0);
    }

    public String classificarZonaDeEntrega() {
        if ("Centro".equalsIgnoreCase(bairro)) {
            return "CENTRAL";
        }
        if ("Ponta Grossa".equalsIgnoreCase(cidade)) {
            return "URBANA";
        }
        return "EXTERNA";
    }

    public boolean pertenceARegiaoDo(Endereco outro) {
        String prefixoCep = cep == null || cep.length() < 3 ? "" : cep.substring(0, 3);
        String prefixoOutro = outro.cep == null || outro.cep.length() < 3 ? "" : outro.cep.substring(0, 3);
        return cidade.equalsIgnoreCase(outro.cidade) && prefixoCep.equals(prefixoOutro);
    }

    public BigDecimal calcularTaxaLocalAte(Endereco destino) {
        double distancia = calcularDistanciaAte(destino);
        double adicionalRegiao = pertenceARegiaoDo(destino) ? 0.0 : 3.50;
        return BigDecimal.valueOf(3.75 + distancia * 1.45 + adicionalRegiao)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
