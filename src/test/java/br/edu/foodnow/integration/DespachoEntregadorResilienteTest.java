package br.edu.foodnow.integration;

import br.edu.foodnow.integration.FakeCourierClient.CourierDispatch;
import br.edu.foodnow.integration.FakeCourierClient.CourierRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class DespachoEntregadorResilienteTest {
    private static final CourierRequest REQUISICAO =
            new CourierRequest(20L, 3.0, "CENTRAL", "-25.1,-50.1");
    private static final CourierDispatch ATRIBUIDO =
            new CourierDispatch("COURIER-20", "DRIVER_ASSIGNED", 4);

    @Test
    void devePreservarRespostaDeSucessoSemNovaTentativa() {
        var provedor = new ProvedorControlado(() -> ATRIBUIDO);

        var despacho = resiliente(provedor).solicitarEntregador(REQUISICAO);

        assertThat(despacho).isEqualTo(ATRIBUIDO);
        assertThat(provedor.chamadas).hasValue(1);
    }

    @Test
    void deveRetentarFalhaTransitoriaComAMesmaChaveDeIdempotencia() {
        var provedor = new ProvedorControlado(
                () -> { throw new IllegalStateException("503 indisponível"); },
                () -> ATRIBUIDO);

        var despacho = resiliente(provedor).solicitarEntregador(REQUISICAO);

        assertThat(despacho).isEqualTo(ATRIBUIDO);
        assertThat(provedor.chamadas).hasValue(2);
        assertThat(provedor.requisicoes).containsOnly(REQUISICAO);
    }

    @Test
    void deveAplicarTimeoutELiberarFluxoComFallback() {
        var provedor = new ProvedorControlado(() -> {
            dormir(Duration.ofSeconds(5));
            return ATRIBUIDO;
        });

        long inicio = System.nanoTime();
        var despacho = resiliente(provedor).solicitarEntregador(REQUISICAO);

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
        assertThat(despacho).isEqualTo(new CourierDispatch(null,
                DespachoEntregadorResiliente.SEM_ENTREGADOR, 0));
        assertThat(provedor.chamadas).hasValue(3);
    }

    @Test
    void deveRetentarRespostaInvalidaAntesDoFallback() {
        var provedor = new ProvedorControlado(
                () -> null,
                () -> new CourierDispatch("X", null, 1),
                () -> ATRIBUIDO);

        assertThat(resiliente(provedor).solicitarEntregador(REQUISICAO)).isEqualTo(ATRIBUIDO);
        assertThat(provedor.chamadas).hasValue(3);
    }

    @Test
    void naoDeveRetentarRespostaValidaSemEntregador() {
        var indisponivel = new CourierDispatch(null, "NO_COURIER_AVAILABLE", 0);
        var provedor = new ProvedorControlado(() -> indisponivel);

        assertThat(resiliente(provedor).solicitarEntregador(REQUISICAO)).isEqualTo(indisponivel);
        assertThat(provedor.chamadas).hasValue(1);
    }

    private DespachoEntregadorResiliente resiliente(FakeCourierClient provedor) {
        return new DespachoEntregadorResiliente(
                provedor, Duration.ofMillis(150), 3, Duration.ZERO);
    }

    private static void dormir(Duration duracao) {
        try {
            Thread.sleep(duracao);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class ProvedorControlado extends FakeCourierClient {
        private final List<Supplier<CourierDispatch>> respostas;
        private final AtomicInteger chamadas = new AtomicInteger();
        private final ConcurrentLinkedQueue<CourierRequest> requisicoes = new ConcurrentLinkedQueue<>();

        @SafeVarargs
        ProvedorControlado(Supplier<CourierDispatch>... respostas) {
            this.respostas = List.of(respostas);
        }

        @Override
        public CourierDispatch solicitarEntregador(CourierRequest request) {
            requisicoes.add(request);
            int indice = Math.min(chamadas.getAndIncrement(), respostas.size() - 1);
            return respostas.get(indice).get();
        }
    }
}
