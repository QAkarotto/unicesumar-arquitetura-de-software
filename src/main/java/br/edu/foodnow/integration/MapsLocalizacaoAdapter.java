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
    private static final long ESPERA_BASE_MS = 50L;

    private final FakeMapsClient mapsClient;

    public MapsLocalizacaoAdapter(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @Override
    public Localizacao buscarCoordenadas(Endereco endereco) {
        var coordenadas = executarComRetry(
                () -> mapsClient.buscarCoordenadas(endereco)
        );

        return new Localizacao(
                Double.parseDouble(coordenadas.lat()),
                Double.parseDouble(coordenadas.lng())
        );
    }

    @Override
    public Rota calcularRota(Localizacao origem, Localizacao destino) {
        var resposta = executarComRetry(
                () -> mapsClient.calcularRota(origem, destino)
        );

        return new Rota(
                resposta.distanceKm(),
                resposta.durationMinutes(),
                resposta.deliveryZone()
        );
    }

    /**
     * Executa uma operação com até três tentativas.
     *
     * Somente falhas identificadas como transitórias são repetidas.
     */
    private <T> T executarComRetry(Supplier<T> operacao) {
        int tentativa = 1;

        while (true) {
            try {
                return operacao.get();

            } catch (FalhaTemporariaMapsException excecao) {

                if (tentativa >= MAX_TENTATIVAS) {
                    throw excecao;
                }

                aguardarProximaTentativa(tentativa);

                tentativa++;
            }
        }
    }

    /**
     * Aplica um intervalo crescente entre as tentativas.
     */
    private void aguardarProximaTentativa(int tentativa) {
        long espera = ESPERA_BASE_MS * tentativa;

        try {
            Thread.sleep(espera);

        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();

            throw new FalhaTemporariaMapsException(
                    "Tentativa de comunicação com o provedor de mapas interrompida.",
                    excecao
            );
        }
    }
}