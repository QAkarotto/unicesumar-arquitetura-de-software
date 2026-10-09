package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Service;

@Service
public class LocalizacaoService {
    private final ProvedorLocalizacao provedor;

    public LocalizacaoService(ProvedorLocalizacao provedor) {
        this.provedor = provedor;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        return provedor.buscarCoordenadas(endereco);
    }

    public Rota calcularRota(Endereco origem, Endereco destino) {
        return provedor.calcularRota(origem.getLocalizacao(), destino.getLocalizacao());
    }

    public double calcularDistancia(Endereco origem, Endereco destino) {
        return calcularRota(origem, destino).distanciaKm();
    }

    public int estimarTempoEntrega(Endereco origem, Endereco destino) {
        return calcularRota(origem, destino).tempoMinutos();
    }
}
