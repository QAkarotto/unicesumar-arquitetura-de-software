package br.edu.foodnow.service;

import br.edu.foodnow.Exceptions.ServicoLocalizacaoIndisponivelException;
import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.integration.FalhaTemporariaMapasException;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void deveRepetirConsultaDeRotaAposFalhaTemporariaEManterResultadoDeSucesso() {
        AtomicInteger chamadas = new AtomicInteger();
        FakeMapsClient client = new FakeMapsClient() {
            @Override
            public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
                if (chamadas.incrementAndGet() == 1) {
                    throw new FalhaTemporariaMapasException("Timeout temporário");
                }
                return super.calcularRota(origem, destino);
            }
        };
        LocalizacaoService service = new LocalizacaoService(client);
        Endereco origem = endereco(new Localizacao(-25.100, -50.150));
        Endereco destino = endereco(new Localizacao(-25.095, -50.160));

        LocalizacaoService.RotaEntrega rota = service.avaliarRota(origem, destino);

        assertThat(chamadas).hasValue(2);
        assertThat(rota.distanciaKm()).isPositive();
        assertThat(rota.zonaEntrega()).isIn("CENTRAL", "EXPANDIDA");
    }

    @Test
    void deveRegistrarFalhaTemporariaERecuperacaoSemDadosDaOperacao() {
        AtomicInteger chamadas = new AtomicInteger();
        FakeMapsClient client = new FakeMapsClient() {
            @Override
            public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
                if (chamadas.incrementAndGet() == 1) {
                    throw new FalhaTemporariaMapasException("Timeout temporário");
                }
                return super.calcularRota(origem, destino);
            }
        };
        Logger logger = (Logger) LoggerFactory.getLogger(LocalizacaoService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            new LocalizacaoService(client).avaliarRota(
                    endereco(new Localizacao(-25.100, -50.150)),
                    endereco(new Localizacao(-25.095, -50.160)));

            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(mensagem -> assertThat(mensagem)
                            .contains("operação=calcularRota", "tentativa=1", "FalhaTemporariaMapasException"))
                    .anySatisfy(mensagem -> assertThat(mensagem)
                            .contains("recuperada", "operação=calcularRota", "tentativas=2"));
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .noneMatch(mensagem -> mensagem.contains("-25.100") || mensagem.contains("-50.150"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void deveFalharComServicoIndisponivelDepoisDoLimiteDeTentativas() {
        AtomicInteger chamadas = new AtomicInteger();
        FakeMapsClient client = new FakeMapsClient() {
            @Override
            public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
                chamadas.incrementAndGet();
                throw new FalhaTemporariaMapasException("Provedor indisponível");
            }
        };
        LocalizacaoService service = new LocalizacaoService(client);
        Endereco origem = endereco(new Localizacao(-25.100, -50.150));
        Endereco destino = endereco(new Localizacao(-25.095, -50.160));

        Logger logger = (Logger) LoggerFactory.getLogger(LocalizacaoService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> service.avaliarRota(origem, destino))
                    .isInstanceOf(ServicoLocalizacaoIndisponivelException.class)
                    .hasMessageContaining("3 tentativas")
                    .hasCauseInstanceOf(FalhaTemporariaMapasException.class);
            assertThat(chamadas).hasValue(3);
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .anySatisfy(mensagem -> assertThat(mensagem)
                            .contains("indisponível após retries", "operação=calcularRota", "tentativas=3"));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void deveInterromperRetryQuandoThreadForInterrompida() {
        FakeMapsClient client = new FakeMapsClient() {
            @Override
            public RouteResult calcularRota(Localizacao origem, Localizacao destino) {
                throw new FalhaTemporariaMapasException("Timeout temporário");
            }
        };
        Thread.currentThread().interrupt();

        try {
            assertThatThrownBy(() -> new LocalizacaoService(client).avaliarRota(
                    endereco(new Localizacao(-25.100, -50.150)),
                    endereco(new Localizacao(-25.095, -50.160))))
                    .isInstanceOf(ServicoLocalizacaoIndisponivelException.class)
                    .hasMessageContaining("interrompida");
        } finally {
            Thread.interrupted();
        }
    }

    private Endereco endereco(Localizacao localizacao) {
        return new Endereco("Rua A", "1", "Centro", "Ponta Grossa", "84000-000", localizacao);
    }
}
