package br.edu.foodnow.entrega;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Restaurante;

/**
 * Entrada da cotação de entrega. É o ponto de extensão para os dados que a futura regra
 * (horário, condições operacionais) vai precisar: novos campos entram aqui, sem alterar
 * a assinatura de quem chama a {@link PoliticaEntrega}.
 */
public record SolicitacaoCotacao(Cliente cliente, Restaurante restaurante, Endereco destino) {
}
