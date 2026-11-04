package br.edu.foodnow.integration;

import br.edu.foodnow.integration.FakeCourierClient.CourierDispatch;
import br.edu.foodnow.integration.FakeCourierClient.CourierRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class DespachoEntregadorResilienteTest {
    private static final CourierRequest REQUISICAO = new CourierRequest(20L, 3.0, "CENTRAL", "-25.1,-50.1");
    private static final CourierDispatch ATRIBUIDO = new CourierDispatch("COURIER-20", "DRIVER_ASSIGNED", 4);

    @Test
    void deveRepassarRespostaDoProvedorQuandoAChamadaFuncionaDePrimeira() {
        var provedor = new ProvedorRoteirizado(() -> ATRIBUIDO);
        var despacho = resiliente(provedor).solicitarEntregador(REQUISICAO);

        assertThat(despacho).isEqualTo(ATRIBUIDO);
        assertThat(provedor.chamadas.get()).isEqualTo(1);
    }

    @Test
    void devePreservarComportamentoDoProvedorSimuladoReal() {
        var despacho = new DespachoEntregadorResiliente(new FakeCourierClient(), Duration.ofSeconds(1), 3,
                Duration.ZERO).solicitarEntregador(REQUISICAO);
        assertThat(despacho).isEqualTo(new FakeCourierClient().solicitarEntregador(REQUISICAO));
    }

    @Test
    void deveRetentarAposFalhaTransitoriaComAMesmaRequisicao() {
        var provedor = new ProvedorRoteirizado(
                () -> { throw new IllegalStateException("503 service unavailable"); },
                () -> ATRIBUIDO);
        var despacho = resiliente(provedor).solicitarEntregador(REQUISICAO);

        assertThat(despacho).isEqualTo(ATRIBUIDO);
        assertThat(provedor.chamadas.get()).isEqualTo(2);
        assertThat(provedor.recebidas).containsOnly(REQUISICAO);
    }

    @Test
    void deveDesistirPorTimeoutERetornarSemEntregador() {
        var provedor = new ProvedorRoteirizado(() -> {
            dormir(Duration.ofSeconds(5));
            return ATRIBUIDO;
        });
        long inicio = System.nanoTime();
        var despacho = resiliente(provedor).solicitarEntregador(REQUISICAO);

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
        assertThat(despacho).isEqualTo(new CourierDispatch(null, DespachoEntregadorResiliente.SEM_ENTREGADOR, 0));
        assertThat(provedor.chamadas.get()).isEqualTo(3);
    }

    @Test
    void deveTratarRespostaInesperadaComoFalha() {
        var provedor = new ProvedorRoteirizado(() -> null, () -> new CourierDispatch("X", null, 1), () -> ATRIBUIDO);
        assertThat(resiliente(provedor).solicitarEntregador(REQUISICAO)).isEqualTo(ATRIBUIDO);
        assertThat(provedor.chamadas.get()).isEqualTo(3);
    }

    @Test
    void naoDeveRetentarQuandoOProvedorRespondeQueNaoHaEntregador() {
        var semEntregador = new CourierDispatch(null, "NO_COURIER_AVAILABLE", 0);
        var provedor = new ProvedorRoteirizado(() -> semEntregador);
        assertThat(resiliente(provedor).solicitarEntregador(REQUISICAO)).isEqualTo(semEntregador);
        assertThat(provedor.chamadas.get()).isEqualTo(1);
    }

    @Test
    void devePararDeTentarQuandoAThreadEInterrompida() {
        var provedor = new ProvedorRoteirizado(() -> { throw new IllegalStateException("falha"); });
        var despachante = new DespachoEntregadorResiliente(provedor, Duration.ofMillis(200), 3, Duration.ofSeconds(5));
        Thread.currentThread().interrupt();
        try {
            assertThat(despachante.solicitarEntregador(REQUISICAO).providerStatus())
                    .isEqualTo(DespachoEntregadorResiliente.SEM_ENTREGADOR);
            assertThat(provedor.chamadas.get()).isLessThanOrEqualTo(1);
        } finally {
            Thread.interrupted();
        }
    }

    private DespachoEntregadorResiliente resiliente(FakeCourierClient provedor) {
        return new DespachoEntregadorResiliente(provedor, Duration.ofMillis(200), 3, Duration.ZERO);
    }

    private static void dormir(Duration duracao) {
        try {
            Thread.sleep(duracao);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class ProvedorRoteirizado extends FakeCourierClient {
        private final List<Supplier<CourierDispatch>> respostas;
        private final AtomicInteger chamadas = new AtomicInteger();
        private final Deque<CourierRequest> recebidas = new ConcurrentLinkedDeque<>();

        @SafeVarargs
        ProvedorRoteirizado(Supplier<CourierDispatch>... respostas) {
            this.respostas = List.of(respostas);
        }

        @Override
        public CourierDispatch solicitarEntregador(CourierRequest request) {
            recebidas.add(request);
            int indice = Math.min(chamadas.getAndIncrement(), respostas.size() - 1);
            return respostas.get(indice).get();
        }
    }
}
