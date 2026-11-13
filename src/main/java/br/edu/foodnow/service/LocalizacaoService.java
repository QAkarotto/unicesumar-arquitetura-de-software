package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.UUID;

@Service
public class LocalizacaoService {

    private static final Logger log =
            LoggerFactory.getLogger(LocalizacaoService.class);

    private final ProvedorLocalizacao provedor;

    public LocalizacaoService(ProvedorLocalizacao provedor) {
        this.provedor = provedor;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        return provedor.buscarCoordenadas(endereco);
    }

    public Rota calcularRota(Endereco origem, Endereco destino) {

        String operacaoId = UUID.randomUUID().toString();

        Localizacao localOrigem = origem.getLocalizacao();
        Localizacao localDestino = destino.getLocalizacao();

        try {

            Rota rota = provedor.calcularRota(
                    localOrigem,
                    localDestino
            );

            log.info(
                    "Operacao calcularRota | ID: {} | Tentativa: 1 | Resultado: sucesso",
                    operacaoId
            );

            return rota;

        } catch (UncheckedIOException erro) {

            if (erro.getCause() instanceof SocketTimeoutException) {

                log.warn(
                        "Operacao calcularRota | ID: {} | Falha: timeout | Resultado: nova tentativa",
                        operacaoId
                );

                try {

                    Rota rota = provedor.calcularRota(
                            localOrigem,
                            localDestino
                    );

                    log.info(
                            "Operacao calcularRota | ID: {} | Tentativa: 2 | Resultado: recuperado",
                            operacaoId
                    );

                    return rota;

                } catch (RuntimeException segundaFalha) {

                    log.error(
                            "Operacao calcularRota | ID: {} | Falha: {} | Resultado: falha definitiva",
                            operacaoId,
                            segundaFalha.getClass().getSimpleName()
                    );

                    throw segundaFalha;
                }
            }

            log.error(
                    "Operacao calcularRota | ID: {} | Falha: {} | Resultado: sem nova tentativa",
                    operacaoId,
                    erro.getClass().getSimpleName()
            );

            throw erro;

        } catch (RuntimeException erro) {

            log.error(
                    "Operacao calcularRota | ID: {} | Falha: {} | Resultado: erro",
                    operacaoId,
                    erro.getClass().getSimpleName()
            );

            throw erro;
        }
    }

    public double calcularDistancia(
            Endereco origem,
            Endereco destino) {

        return calcularRota(origem, destino).distanciaKm();
    }

    public int estimarTempoEntrega(
            Endereco origem,
            Endereco destino) {

        return calcularRota(origem, destino).tempoMinutos();
    }
}