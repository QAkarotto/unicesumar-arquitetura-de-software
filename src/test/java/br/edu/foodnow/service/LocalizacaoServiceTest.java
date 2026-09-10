package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalizacaoServiceTest {

    @Test
    void deveCentralizarDadosDeRotaEZona() {
        LocalizacaoService service = new LocalizacaoService(new FakeMapsClient());
        Endereco origem = new Endereco("Rua A", "1", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.100, -50.150));
        Endereco destino = new Endereco("Rua B", "3", "Centro", "Ponta Grossa", "84000-000",
                new Localizacao(-25.095, -50.160));

        LocalizacaoService.RotaEntrega rota = service.avaliarRota(origem, destino);

        assertThat(rota.distanciaKm()).isPositive();
        assertThat(rota.tempoEstimadoMinutos()).isPositive();
        assertThat(rota.zonaEntrega()).isIn("CENTRAL", "EXPANDIDA");
        assertThat(rota.origemFormatada()).isEqualTo("-25.100000,-50.150000");
        assertThat(rota.destinoFormatada()).isEqualTo("-25.095000,-50.160000");
    }
}
