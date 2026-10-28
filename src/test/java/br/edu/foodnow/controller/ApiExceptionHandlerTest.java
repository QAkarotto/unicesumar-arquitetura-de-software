package br.edu.foodnow.controller;

import org.junit.jupiter.api.Test;

import br.edu.foodnow.Exceptions.ApiExceptionHandler;
import br.edu.foodnow.Exceptions.ServicoLocalizacaoIndisponivelException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    @Test
    void deveConverterFalhaExauridaDeLocalizacaoEmServicoIndisponivel() {
        ApiDtos.ErroResponse resposta = new ApiExceptionHandler().servicoIndisponivel(
                new ServicoLocalizacaoIndisponivelException("Mapas indisponíveis", new RuntimeException()));

        assertThat(resposta.status()).isEqualTo(503);
        assertThat(resposta.erro()).isEqualTo("SERVICO_INDISPONIVEL");
        assertThat(resposta.mensagem()).isEqualTo("Mapas indisponíveis");
    }
}
