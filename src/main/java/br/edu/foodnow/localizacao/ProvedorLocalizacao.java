package br.edu.foodnow.localizacao;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;

public interface ProvedorLocalizacao {
    Localizacao buscarCoordenadas(Endereco endereco);

    Rota calcularRota(Localizacao origem, Localizacao destino);
}
