package br.edu.foodnow.geo;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;

/**
 * Porta de saída para qualquer serviço geográfico (mapas, geocoding, rotas).
 * <p>
 * A aplicação depende somente desta interface; o provedor concreto
 * (hoje {@code FakeMapsClient}) fica isolado em um adapter no pacote {@code integration}.
 */
public interface ProvedorGeografico {

    CoordenadasResolvidas buscarCoordenadas(Endereco endereco);

    Rota calcularRota(Localizacao origem, Localizacao destino);
}
