package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Service;

@Service
public class LocalizacaoService {
    private final FakeMapsClient mapsClient;

    public LocalizacaoService(FakeMapsClient mapsClient) {
        this.mapsClient = mapsClient;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        FakeMapsClient.MapCoordinates resposta = mapsClient.buscarCoordenadas(endereco);
        return new Localizacao(Double.parseDouble(resposta.lat()), Double.parseDouble(resposta.lng()));
    }

    public RotaEntrega avaliarRota(Endereco origem, Endereco destino) {
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(origem.getLocalizacao(), destino.getLocalizacao());
        return new RotaEntrega(rota.distanceKm(), rota.durationMinutes(), rota.deliveryZone(),
                origem.getLocalizacao().formatarParaProvedor(), destino.getLocalizacao().formatarParaProvedor());
    }

    public double calcularDistancia(Endereco origem, Endereco destino) {
        return avaliarRota(origem, destino).distanciaKm();
    }

    public int estimarTempoEntrega(Endereco origem, Endereco destino) {
        return avaliarRota(origem, destino).tempoEstimadoMinutos();
    }

    public record RotaEntrega(double distanciaKm, int tempoEstimadoMinutos, String zonaEntrega,
                             String origemFormatada, String destinoFormatada) {
    }
}
