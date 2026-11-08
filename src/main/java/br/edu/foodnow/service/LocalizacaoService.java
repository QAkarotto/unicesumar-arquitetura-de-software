package br.edu.foodnow.service;

import br.edu.foodnow.Exceptions.ServicoLocalizacaoIndisponivelException;
import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.integration.FalhaTemporariaMapasException;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class LocalizacaoService {
    private static final int MAX_TENTATIVAS = 3;
    private static final Logger log = LoggerFactory.getLogger(LocalizacaoService.class);
    private final FakeMapsClient mapsClient;

    public LocalizacaoService(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        FakeMapsClient.MapCoordinates resposta = executarComRetry(
                "buscarCoordenadas", () -> mapsClient.buscarCoordenadas(endereco));
        return new Localizacao(Double.parseDouble(resposta.lat()), Double.parseDouble(resposta.lng()));
    }

    public RotaEntrega avaliarRota(Endereco origem, Endereco destino) {
        FakeMapsClient.RouteResult rota = executarComRetry("calcularRota",
                () -> mapsClient.calcularRota(origem.getLocalizacao(), destino.getLocalizacao()));
        return new RotaEntrega(rota.distanceKm(), rota.durationMinutes(), rota.deliveryZone(),
                origem.getLocalizacao().formatarParaProvedor(), destino.getLocalizacao().formatarParaProvedor());
    }

    public double calcularDistancia(Endereco origem, Endereco destino) {
        return avaliarRota(origem, destino).distanciaKm();
    }

    public int estimarTempoEntrega(Endereco origem, Endereco destino) {
        return avaliarRota(origem, destino).tempoEstimadoMinutos();
    }

    private <T> T executarComRetry(String nomeOperacao, Supplier<T> operacao) {
        FalhaTemporariaMapasException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                T resultado = operacao.get();
                if (tentativa > 1) {
                    log.info("Integração de localização recuperada: operação={}, tentativas={}",
                            nomeOperacao, tentativa);
                }
                return resultado;
            } catch (FalhaTemporariaMapasException falha) {
                ultimaFalha = falha;
                log.warn("Falha transitória na integração de localização: operação={}, tentativa={}, limite={}, tipo={}",
                        nomeOperacao, tentativa, MAX_TENTATIVAS, falha.getClass().getSimpleName());
                if (tentativa < MAX_TENTATIVAS) {
                    try {
                        Thread.sleep(50L * tentativa);
                    } catch (InterruptedException interrompido) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrompido: operação={}, tentativas={}, tipo={}",
                                nomeOperacao, tentativa, interrompido.getClass().getSimpleName());
                        throw new ServicoLocalizacaoIndisponivelException(
                                "Consulta ao serviço de localização interrompida", interrompido);
                    }
                }
            }
        }
        log.error("Integração de localização indisponível após retries: operação={}, tentativas={}, tipo={}",
                nomeOperacao, MAX_TENTATIVAS, ultimaFalha.getClass().getSimpleName());
        throw new ServicoLocalizacaoIndisponivelException(
                "Serviço de localização indisponível após " + MAX_TENTATIVAS + " tentativas", ultimaFalha);
    }

    public record RotaEntrega(double distanciaKm, int tempoEstimadoMinutos, String zonaEntrega,
                             String origemFormatada, String destinoFormatada) {
    }
}
