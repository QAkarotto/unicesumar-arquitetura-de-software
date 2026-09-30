package br.edu.foodnow.service;

import br.edu.foodnow.geo.ProvedorGeografico;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Service;

@Service
public class LocalizacaoService {
    private final ProvedorGeografico provedorGeografico;

    public LocalizacaoService(ProvedorGeografico provedorGeografico) {
        this.provedorGeografico = provedorGeografico;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        return provedorGeografico.buscarCoordenadas(endereco).localizacao();
    }
}
