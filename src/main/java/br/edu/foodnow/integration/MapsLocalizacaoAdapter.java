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
        var coordenadas = consultarComRepeticao(() -> mapsClient.buscarCoordenadas(endereco));
        return new Localizacao(Double.parseDouble(coordenadas.lat()), Double.parseDouble(coordenadas.lng()));
    }

    @Override
    public Rota calcularRota(Localizacao origem, Localizacao destino) {
        var resposta = consultarComRepeticao(() -> mapsClient.calcularRota(origem, destino));
        return new Rota(resposta.distanceKm(), resposta.durationMinutes(), resposta.deliveryZone());
    }

    private <T> T consultarComRepeticao(Supplier<T> consulta) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return consulta.get();
            } catch (MapsIndisponivelException falha) {
                if (tentativa == MAX_TENTATIVAS) {
                    throw falha;
                }
            }
        }
    }

}
