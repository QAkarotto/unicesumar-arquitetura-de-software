package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class MapsLocalizacaoAdapter implements ProvedorLocalizacao {
    private static final int MAX_TENTATIVAS = 3;

    private final FakeMapsClient mapsClient;

    public MapsLocalizacaoAdapter(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @Override
    public Localizacao buscarCoordenadas(Endereco endereco) {
        var coordenadas = executarComRetry(() -> mapsClient.buscarCoordenadas(endereco));
        try {
            return new Localizacao(Double.parseDouble(coordenadas.lat()), Double.parseDouble(coordenadas.lng()));
        } catch (NumberFormatException | NullPointerException excecao) {
            throw new IllegalStateException("O provedor de mapas retornou coordenadas inválidas.", excecao);
        }
    }

    @Override
    public Rota calcularRota(Localizacao origem, Localizacao destino) {
        var resposta = executarComRetry(() -> mapsClient.calcularRota(origem, destino));
        return new Rota(resposta.distanceKm(), resposta.durationMinutes(), resposta.deliveryZone());
    }

    /** Repete apenas falhas classificadas como temporárias e limita o total de chamadas. */
    private <T> T executarComRetry(Supplier<T> consulta) {
        FalhaTemporariaMapsException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                return consulta.get();
            } catch (FalhaTemporariaMapsException excecao) {
                ultimaFalha = excecao;
            }
        }
        throw new FalhaTemporariaMapsException(
                "Provedor de mapas indisponível após " + MAX_TENTATIVAS + " tentativas.", ultimaFalha);
    }
}
