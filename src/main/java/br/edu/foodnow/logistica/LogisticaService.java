package br.edu.foodnow.logistica;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Pedido;
import br.edu.foodnow.model.Restaurante;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Fronteira dos casos de uso para cotação, planejamento e resumo logístico do pedido. */
@Service
public class LogisticaService {
    private final ProvedorLocalizacao localizacao;
    private final PoliticaEntregaAtual politica;

    public LogisticaService(ProvedorLocalizacao localizacao, PoliticaEntregaAtual politica) {
        this.localizacao = localizacao;
        this.politica = politica;
    }

    public PoliticaEntregaAtual.CotacaoPedido cotarPedido(Cliente cliente, Restaurante restaurante, Endereco destino) {
        var rota = localizacao.calcularRota(restaurante.getEndereco().getLocalizacao(), destino.getLocalizacao());
        return politica.cotarPedido(cliente, restaurante, destino, rota);
    }

    public PoliticaEntregaAtual.PlanoEntrega planejarEntrega(Pedido pedido) {
        var rota = localizacao.calcularRota(pedido.getRestaurante().getEndereco().getLocalizacao(),
                pedido.getEnderecoEntrega().getLocalizacao());
        return politica.planejarEntrega(pedido, rota);
    }

    public BigDecimal adicionalDosItens(Pedido pedido) {
        return politica.adicionalDosItens(pedido);
    }

    public PoliticaEntregaAtual.ResumoPedido resumirPedido(Pedido pedido) {
        return politica.resumirPedido(pedido);
    }
}
