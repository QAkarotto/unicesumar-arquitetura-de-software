package br.edu.foodnow.integration;

import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.service.LocalizacaoService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MapsLocalizacaoAdapterTest {
    private final LocalizacaoService localizacao = new LocalizacaoService(new MapsLocalizacaoAdapter(new FakeMapsClient()));

    @Test
    void deveTraduzirCoordenadasERotaSemExporRecordsDoProvedor() {
        Endereco origem = endereco(new Localizacao(-25.100, -50.150));
        Endereco destino = endereco(new Localizacao(-25.095, -50.160));
        assertThat(localizacao.buscarCoordenadas(origem).getLatitude()).isEqualTo(-25.100);
        assertThat(localizacao.buscarCoordenadas(origem).getLongitude()).isEqualTo(-50.150);
        assertThat(localizacao.calcularRota(origem, destino)).isEqualTo(new Rota(1.32, 17, "CENTRAL"));
        assertThat(localizacao.calcularDistancia(origem, destino)).isEqualTo(1.32);
        assertThat(localizacao.estimarTempoEntrega(origem, destino)).isEqualTo(17);
    }

    @Test
    void devePreservarGeocodificacaoAproximadaQuandoNaoHaCoordenadas() {
        Endereco endereco = endereco(null);
        var esperado = new FakeMapsClient().buscarCoordenadas(endereco);
        var coordenadas = localizacao.buscarCoordenadas(endereco);
        assertThat(coordenadas.getLatitude()).isEqualTo(Double.parseDouble(esperado.lat()));
        assertThat(coordenadas.getLongitude()).isEqualTo(Double.parseDouble(esperado.lng()));
    }

    @Test
    void deveRepetirConsultaAposFalhasTemporariasEConcluirQuandoProvedorSeRecuperar() {
        AtomicInteger tentativas = new AtomicInteger();
        FakeMapsClient clienteInstavel = new FakeMapsClient() {
            @Override
            public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
                if (tentativas.incrementAndGet() < 3) {
                    throw new FalhaTemporariaMapsException("Timeout temporário simulado.");
                }
                return new RouteResult(4.5, 20, "CENTRAL");
            }
        };
        MapsLocalizacaoAdapter adapter = new MapsLocalizacaoAdapter(clienteInstavel);

        Rota rota = adapter.calcularRota(new Localizacao(-25.1, -50.1), new Localizacao(-25.2, -50.2));

        assertThat(rota).isEqualTo(new Rota(4.5, 20, "CENTRAL"));
        assertThat(tentativas.get()).isEqualTo(3);
    }

    @Test
    void deveEncerrarComErroClaroAposTresFalhasTemporarias() {
        AtomicInteger tentativas = new AtomicInteger();
        FakeMapsClient clienteIndisponivel = new FakeMapsClient() {
            @Override
            public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
                tentativas.incrementAndGet();
                throw new FalhaTemporariaMapsException("Provedor temporariamente indisponível.");
            }
        };
        MapsLocalizacaoAdapter adapter = new MapsLocalizacaoAdapter(clienteIndisponivel);

        assertThatThrownBy(() -> adapter.calcularRota(
                new Localizacao(-25.1, -50.1), new Localizacao(-25.2, -50.2)))
                .isInstanceOf(FalhaTemporariaMapsException.class)
                .hasMessageContaining("após 3 tentativas");
        assertThat(tentativas.get()).isEqualTo(3);
    }

    @Test
    void deveInformarQuandoProvedorRetornaCoordenadasInvalidas() {
        FakeMapsClient clienteComRespostaInvalida = new FakeMapsClient() {
            @Override
            public MapCoordinates buscarCoordenadas(Endereco endereco) {
                return new MapCoordinates("latitude-invalida", "-50.1", "APPROXIMATE");
            }
        };
        MapsLocalizacaoAdapter adapter = new MapsLocalizacaoAdapter(clienteComRespostaInvalida);

        assertThatThrownBy(() -> adapter.buscarCoordenadas(endereco(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("coordenadas inválidas");
    }

    private Endereco endereco(Localizacao coordenadas) {
        return new Endereco("Rua Integração", "1", "Centro", "Ponta Grossa", "84000-000", coordenadas);
    }
}
