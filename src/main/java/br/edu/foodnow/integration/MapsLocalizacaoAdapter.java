package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Component;

@Component
public class MapsLocalizacaoAdapter implements ProvedorLocalizacao {
    private final FakeMapsClient mapsClient;

    public MapsLocalizacaoAdapter(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    @Override
    public Localizacao buscarCoordenadas(Endereco endereco) {
        var coordenadas = tentarNovamente(() -> mapsClient.buscarCoordenadas(endereco));
        return new Localizacao(Double.parseDouble(coordenadas.lat()), Double.parseDouble(coordenadas.lng()));
    }

    @Override
    public Rota calcularRota(Localizacao origem, Localizacao destino) {
        var resposta = tentarNovamente(() -> mapsClient.calcularRota(origem, destino));
        return new Rota(resposta.distanceKm(), resposta.durationMinutes(), resposta.deliveryZone());
    }

    private <T> T tentarNovamente(java.util.function.Supplier<T> consulta) {
        try {
            return consulta.get();
        } catch (RuntimeException primeiraFalha) {
            return consulta.get();
        }
    }

}
