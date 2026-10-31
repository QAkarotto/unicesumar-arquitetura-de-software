
package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalizacaoServiceRetryTest {

    @Mock
    private ProvedorLocalizacao provedor;

    @Mock
    private Endereco origem;

    @Mock
    private Endereco destino;

    private LocalizacaoService service;

    private final Localizacao localOrigem =
            new Localizacao(-25.1, -50.1);

    private final Localizacao localDestino =
            new Localizacao(-25.2, -50.2);

    @BeforeEach
    void preparar() {
        service = new LocalizacaoService(provedor);

        when(origem.getLocalizacao())
                .thenReturn(localOrigem);

        when(destino.getLocalizacao())
                .thenReturn(localDestino);
    }

    @Test
    void deveCalcularRotaNormalmente() {

        Rota rota = new Rota(3.5, 20, "CENTRAL");

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenReturn(rota);

        Rota resultado = service.calcularRota(origem, destino);

        assertEquals(rota, resultado);

        verify(provedor, times(1))
                .calcularRota(localOrigem, localDestino);
    }

    @Test
    void deveTentarNovamenteAposTimeout() {

        Rota rota = new Rota(3.5, 20, "CENTRAL");

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenThrow(new UncheckedIOException(
                        new SocketTimeoutException("Timeout")))
                .thenReturn(rota);

        Rota resultado = service.calcularRota(origem, destino);

        assertEquals(rota, resultado);

        verify(provedor, times(2))
                .calcularRota(localOrigem, localDestino);
    }

    @Test
    void devePararDepoisDeDuasFalhas() {

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenThrow(new UncheckedIOException(
                        new SocketTimeoutException("Timeout")));

        assertThrows(UncheckedIOException.class,
                () -> service.calcularRota(origem, destino));

        verify(provedor, times(2))
                .calcularRota(localOrigem, localDestino);
    }

    @Test
    void naoDeveRepetirErroDiferenteDeTimeout() {

        when(provedor.calcularRota(localOrigem, localDestino))
                .thenThrow(new IllegalArgumentException(
                        "Coordenadas invalidas"));

        assertThrows(IllegalArgumentException.class,
                () -> service.calcularRota(origem, destino));

        verify(provedor, times(1))
                .calcularRota(localOrigem, localDestino);
    }
}
