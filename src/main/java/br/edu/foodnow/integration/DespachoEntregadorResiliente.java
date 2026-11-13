package br.edu.foodnow.integration;

import br.edu.foodnow.integration.FakeCourierClient.CourierDispatch;
import br.edu.foodnow.integration.FakeCourierClient.CourierRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Protege o caso de uso contra indisponibilidade, lentidão e respostas inválidas do provedor. */
@Component
public class DespachoEntregadorResiliente {
    static final String SEM_ENTREGADOR = "NO_COURIER_AVAILABLE";
    private static final Logger logger = LoggerFactory.getLogger(DespachoEntregadorResiliente.class);

    private final FakeCourierClient courierClient;
    private final Duration timeout;
    private final int maximoTentativas;
    private final Duration intervalo;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public DespachoEntregadorResiliente(FakeCourierClient courierClient,
                                        @Value("${foodnow.courier.timeout:2s}") Duration timeout,
                                        @Value("${foodnow.courier.tentativas:3}") int maximoTentativas,
                                        @Value("${foodnow.courier.intervalo:200ms}") Duration intervalo) {
        this.courierClient = courierClient;
        this.timeout = timeout;
        this.maximoTentativas = maximoTentativas;
        this.intervalo = intervalo;
    }

    public CourierDispatch solicitarEntregador(CourierRequest requisicao) {
        for (int tentativa = 1; tentativa <= maximoTentativas; tentativa++) {
            try {
                return executarComTimeout(requisicao);
            } catch (TimeoutException | ExecutionException | IllegalStateException e) {
                logger.warn("Falha no despacho do pedido {} (tentativa {}/{}): {}",
                        requisicao.orderId(), tentativa, maximoTentativas, e.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (tentativa < maximoTentativas && !aguardarAntesDaProximaTentativa(tentativa)) {
                break;
            }
        }

        return new CourierDispatch(null, SEM_ENTREGADOR, 0);
    }

    private CourierDispatch executarComTimeout(CourierRequest requisicao)
            throws InterruptedException, ExecutionException, TimeoutException {
        Future<CourierDispatch> chamada = executor.submit(
                () -> courierClient.solicitarEntregador(requisicao));
        try {
            CourierDispatch despacho = chamada.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (despacho == null || despacho.providerStatus() == null) {
                throw new IllegalStateException("Resposta inválida do provedor de entregadores");
            }
            return despacho;
        } finally {
            chamada.cancel(true);
        }
    }

    private boolean aguardarAntesDaProximaTentativa(int tentativa) {
        try {
            Thread.sleep(intervalo.multipliedBy(tentativa));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
