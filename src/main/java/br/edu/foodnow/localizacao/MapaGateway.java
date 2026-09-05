package br.edu.foodnow.localizacao;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;

/**
 * Porta de saída para consulta geográfica.
 *
 * <p>Declarada do lado da aplicação e implementada na borda, de forma que trocar o fornecedor de
 * mapas seja uma troca de adaptador e não uma alteração espalhada pelos serviços.</p>
 */
public interface MapaGateway {

    Geocodificacao geocodificar(Endereco endereco);

    Rota calcularRota(Localizacao origem, Localizacao destino);
}
