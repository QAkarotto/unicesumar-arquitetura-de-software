package br.edu.foodnow.pagamento;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.StatusPagamento;

import java.math.BigDecimal;

/** Contrato da aplicação para autorizar um pagamento, independente do provedor. */
public interface AutorizadorPagamento {
    Resultado autorizar(Solicitacao solicitacao);

    record Solicitacao(Long pedidoId, BigDecimal valor, FormaPagamento forma, String token,
                       String regiao, Localizacao origem, Localizacao destino) {
    }

    record Resultado(StatusPagamento status, String codigo, String mensagem) {
    }
}
