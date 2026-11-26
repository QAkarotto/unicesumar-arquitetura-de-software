package br.edu.foodnow.integration;

import br.edu.foodnow.model.FormaPagamento;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.StatusPagamento;
import br.edu.foodnow.pagamento.AutorizadorPagamento;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AutorizadorPagamentoAdapterTest {
    private final FakePaymentGateway gateway = mock(FakePaymentGateway.class);
    private final AutorizadorPagamentoAdapter adapter = new AutorizadorPagamentoAdapter(gateway);

    @Test
    void deveTraduzirValorFormaECoordenadasParaOProvedor() {
        when(gateway.processarPagamento(any())).thenReturn(
                new FakePaymentGateway.GatewayResult("FN-15", "AUTHORIZED", "Autorizado"));

        adapter.autorizar(solicitacao());

        verify(gateway).processarPagamento(new FakePaymentGateway.GatewayRequest("15", 5510,
                "token-do-teste", "PIX", "CENTRAL", "-25.100000,-50.150000", "-25.095000,-50.160000"));
    }

    @ParameterizedTest
    @CsvSource({"AUTHORIZED, APROVADO", "DECLINED, REJEITADO", "DESCONHECIDO, REJEITADO"})
    void devePreservarInterpretacaoDoStatusECamposDaResposta(String externo, StatusPagamento esperado) {
        when(gateway.processarPagamento(any())).thenReturn(
                new FakePaymentGateway.GatewayResult("CODIGO-15", externo, "Mensagem do provedor"));

        var resultado = adapter.autorizar(solicitacao());

        assertThat(resultado).isEqualTo(new AutorizadorPagamento.Resultado(
                esperado, "CODIGO-15", "Mensagem do provedor"));
    }

    private AutorizadorPagamento.Solicitacao solicitacao() {
        return new AutorizadorPagamento.Solicitacao(15L, new BigDecimal("55.10"), FormaPagamento.PIX,
                "token-do-teste", "CENTRAL", new Localizacao(-25.100, -50.150),
                new Localizacao(-25.095, -50.160));
    }
}
