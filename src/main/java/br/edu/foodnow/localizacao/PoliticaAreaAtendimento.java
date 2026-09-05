package br.edu.foodnow.localizacao;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Restaurante;
import org.springframework.stereotype.Component;

/**
 * Dono único da decisão sobre atender ou não um endereço.
 *
 * <p>A criação de pedido testava três critérios diferentes na mesma expressão, o raio contra a
 * distância reconciliada, a área de entrega calculada pelo cliente a partir do endereço principal
 * e a cobertura declarada pelo restaurante, que combina raio com cidade e prefixo de CEP. Os três
 * continuam valendo, com a mesma ordem de avaliação, mas agora como política nomeada.</p>
 *
 * <p>É aqui que uma cobertura por região e janela de horário deve entrar, sem espalhar a decisão
 * de volta pelas entidades.</p>
 */
@Component
public class PoliticaAreaAtendimento {

    public boolean atende(Cliente cliente, Restaurante restaurante, Endereco destino,
                          double distanciaReconciliadaKm) {
        return distanciaReconciliadaKm <= restaurante.getRaioEntregaKm()
                && cliente.estaDentroDaAreaDeEntrega(restaurante)
                && restauranteCobre(restaurante, destino);
    }

    public boolean restauranteCobre(Restaurante restaurante, Endereco destino) {
        return restaurante.atendeEndereco(destino);
    }
}
