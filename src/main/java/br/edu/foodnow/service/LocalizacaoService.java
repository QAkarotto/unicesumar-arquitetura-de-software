package br.edu.foodnow.service;

import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.springframework.stereotype.Service;

@Service
public class LocalizacaoService {

    private final DistanceProvider distanceProvider;

    public LocalizacaoService(DistanceProvider distanceProvider) {
        this.distanceProvider = distanceProvider;
    }

    public Localizacao buscarCoordenadas(Endereco endereco) {
        DistanceProvider.MapCoordinates resposta =
                distanceProvider.buscarCoordenadas(endereco);

        return new Localizacao(
                Double.parseDouble(resposta.lat()),
                Double.parseDouble(resposta.lng())
        );
    }

    public DistanceProvider.Route calcularRota(
            Endereco origem,
            Endereco destino) {

        return distanceProvider.calcularRota(origem, destino);
    }

    public double calcularDistancia(
            Endereco origem,
            Endereco destino) {

        return calcularRota(origem, destino).distanceKm();
    }

    public int estimarTempoEntrega(
            Endereco origem,
            Endereco destino) {

        return calcularRota(origem, destino).durationMinutes();
    }
}