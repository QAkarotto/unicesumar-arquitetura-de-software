
package br.edu.foodnow.service;

import br.edu.foodnow.integration.MapsClient;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Service;

@Service
public class LocalizacaoService {
    private final MapsClient mapsClient;

    public LocalizacaoService(MapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        MapsClient.MapCoordinates resultado = mapsClient.buscarCoordenadas(endereco);
        return new Localizacao(Double.parseDouble(resultado.lat()), Double.parseDouble(resultado.lng()));
    }

    public double calcularDistancia(Endereco origem, Endereco destino) {
        MapsClient.RouteResult resultado = mapsClient.calcularRota(origem.getLocalizacao(), destino.getLocalizacao());
        return resultado.distanceKm();
    }

    public int estimarTempoEntrega(Endereco origem, Endereco destino) {
        MapsClient.RouteResult resultado = mapsClient.calcularRota(origem.getLocalizacao(), destino.getLocalizacao());
        return resultado.durationMinutes();
    }

    public String buscarZonaEntrega(Endereco origem, Endereco destino) {
        MapsClient.RouteResult resultado = mapsClient.calcularRota(origem.getLocalizacao(), destino.getLocalizacao());
        return resultado.deliveryZone();
    }
}
