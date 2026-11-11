package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.ProvedorLocalizacao;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

@Component
public class MapsLocalizacaoAdapter implements ProvedorLocalizacao {
    private static final Logger log = LoggerFactory.getLogger(MapsLocalizacaoAdapter.class);
    private static final int MAX_TENTATIVAS = 3;
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
        String operacaoId = UUID.randomUUID().toString();
        for (int tentativa = 1; ; tentativa++) {
            try {
                var resposta = mapsClient.calcularRota(origem, destino);
                Rota rota = new Rota(resposta.distanceKm(), resposta.durationMinutes(), resposta.deliveryZone());
                log.info("operacao=calcular_rota operacaoId={} tentativa={} limite={} resultado=sucesso",
                        operacaoId, tentativa, MAX_TENTATIVAS);
                return rota;
            } catch (ResourceAccessException excecao) {
                if (tentativa >= MAX_TENTATIVAS) {
                    log.error("operacao=calcular_rota operacaoId={} tentativa={} limite={} "
                                    + "tipoFalha={} resultado=tentativas_esgotadas",
                            operacaoId, tentativa, MAX_TENTATIVAS, excecao.getClass().getSimpleName());
                    throw excecao;
                }
                // Consultar uma rota não produz efeitos de negócio que possam ser duplicados.
                log.warn("operacao=calcular_rota operacaoId={} tentativa={} limite={} "
                                + "tipoFalha={} resultado=nova_tentativa",
                        operacaoId, tentativa, MAX_TENTATIVAS, excecao.getClass().getSimpleName());
            } catch (RuntimeException excecao) {
                log.error("operacao=calcular_rota operacaoId={} tentativa={} limite={} "
                                + "tipoFalha={} resultado=falha_nao_repetivel",
                        operacaoId, tentativa, MAX_TENTATIVAS, excecao.getClass().getSimpleName());
                throw excecao;
            }
        }
    }
}
