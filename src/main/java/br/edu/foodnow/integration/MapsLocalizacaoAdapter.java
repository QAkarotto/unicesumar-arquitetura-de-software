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
        var coordenadas = mapsClient.buscarCoordenadas(endereco);
        return new Localizacao(Double.parseDouble(coordenadas.lat()), Double.parseDouble(coordenadas.lng()));
    }

    @Override
    public Rota calcularRota(Localizacao origem, Localizacao destino) {
        var resposta = mapsClient.calcularRota(origem, destino);
        return new Rota(resposta.distanceKm(), resposta.durationMinutes(), resposta.deliveryZone());
    }
}
