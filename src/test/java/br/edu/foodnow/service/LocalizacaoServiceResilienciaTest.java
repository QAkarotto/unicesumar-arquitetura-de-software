package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class LocalizacaoServiceResilienciaTest {

    private ProvedorLocalizacao provedor;
    private LocalizacaoService service;

    private Endereco origem;
    private Endereco destino;

    private Localizacao localOrigem;
    private Localizacao localDestino;

    @BeforeEach
    void preparar() {

        provedor = mock(ProvedorLocalizacao.class);
        service = new LocalizacaoService(provedor);

        origem = mock(Endereco.class);
        destino = mock(Endereco.class);

        localOrigem = new Localizacao(-25.1, -50.1);
        localDestino = new Localizacao(-25.2, -50.2);

        when(origem.getLocalizacao())
                .thenReturn(localOrigem);

        when(destino.getLocalizacao())
                .thenReturn(localDestino);
    }

    @Test
    void deveCalcularRotaNormalmente(CapturedOutput output) {

        Rota rota = new Rota(3.5, 20, "CENTRAL");

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenReturn(rota);

        Rota resultado = service.calcularRota(origem, destino);

        assertEquals(rota, resultado);

        verify(provedor, times(1))
                .calcularRota(localOrigem, localDestino);

        assertTrue(
                output.getAll().contains("Resultado: sucesso")
        );
    }

    @Test
    void deveRecuperarAposTimeout(CapturedOutput output) {

        Rota rota = new Rota(3.5, 20, "CENTRAL");

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenThrow(new UncheckedIOException(
                        new SocketTimeoutException("Timeout")))
                .thenReturn(rota);

        Rota resultado = service.calcularRota(origem, destino);

        assertEquals(rota, resultado);

        verify(provedor, times(2))
                .calcularRota(localOrigem, localDestino);

        assertTrue(
                output.getAll().contains("Falha: timeout")
        );

        assertTrue(
                output.getAll().contains("Resultado: recuperado")
        );
    }

    @Test
    void devePararDepoisDeDuasFalhas(CapturedOutput output) {

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenThrow(new UncheckedIOException(
                        new SocketTimeoutException("Timeout")));

        assertThrows(
                UncheckedIOException.class,
                () -> service.calcularRota(origem, destino)
        );

        verify(provedor, times(2))
                .calcularRota(localOrigem, localDestino);

        assertTrue(
                output.getAll().contains("falha definitiva")
        );
    }

    @Test
    void naoDeveRepetirErroDiferenteDeTimeout(
            CapturedOutput output) {

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenThrow(
                        new IllegalArgumentException(
                                "Coordenadas invalidas")
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularRota(origem, destino)
        );

        verify(provedor, times(1))
                .calcularRota(localOrigem, localDestino);

        assertTrue(
                output.getAll().contains(
                        "Falha: IllegalArgumentException"
                )
        );
    }
}