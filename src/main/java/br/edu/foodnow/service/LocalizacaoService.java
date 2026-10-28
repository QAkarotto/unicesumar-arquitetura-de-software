package br.edu.foodnow.service;

import br.edu.foodnow.Exceptions.ServicoLocalizacaoIndisponivelException;
import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.integration.FalhaTemporariaMapasException;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class LocalizacaoService {
    private static final int MAX_TENTATIVAS = 3;
    private final FakeMapsClient mapsClient;

    public LocalizacaoService(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        FakeMapsClient.MapCoordinates resposta = executarComRetry(() -> mapsClient.buscarCoordenadas(endereco));
        return new Localizacao(Double.parseDouble(resposta.lat()), Double.parseDouble(resposta.lng()));
    }

    public RotaEntrega avaliarRota(Endereco origem, Endereco destino) {
        FakeMapsClient.RouteResult rota = executarComRetry(
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

    private <T> T executarComRetry(Supplier<T> operacao) {
        FalhaTemporariaMapasException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                return operacao.get();
            } catch (FalhaTemporariaMapasException falha) {
                ultimaFalha = falha;
                if (tentativa < MAX_TENTATIVAS) {
                    try {
                        Thread.sleep(50L * tentativa);
                    } catch (InterruptedException interrompido) {
                        Thread.currentThread().interrupt();
                        throw new ServicoLocalizacaoIndisponivelException(
                                "Consulta ao serviço de localização interrompida", interrompido);
                    }
                }
            }
        }
        throw new ServicoLocalizacaoIndisponivelException(
                "Serviço de localização indisponível após " + MAX_TENTATIVAS + " tentativas", ultimaFalha);
    }

    public record RotaEntrega(double distanciaKm, int tempoEstimadoMinutos, String zonaEntrega,
                             String origemFormatada, String destinoFormatada) {
    }
}
