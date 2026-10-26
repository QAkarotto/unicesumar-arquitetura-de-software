package br.edu.foodnow.integration;

import br.edu.foodnow.integration.FakeCourierClient.CourierDispatch;
import br.edu.foodnow.integration.FakeCourierClient.CourierRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class DespachoEntregadorResiliente {
    static final String SEM_ENTREGADOR = "NO_COURIER_AVAILABLE";
    private static final Logger log = LoggerFactory.getLogger(DespachoEntregadorResiliente.class);

    private final FakeCourierClient courierClient;
    private final Duration timeout;
    private final int tentativas;
    private final Duration intervalo;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    public DespachoEntregadorResiliente(FakeCourierClient courierClient,
                                        @Value("${foodnow.courier.timeout:2s}") Duration timeout,
                                        @Value("${foodnow.courier.tentativas:3}") int tentativas,
                                        @Value("${foodnow.courier.intervalo:200ms}") Duration intervalo) {
        this.courierClient = courierClient;
        this.timeout = timeout;
        this.tentativas = tentativas;
        this.intervalo = intervalo;
    }

    // o orderId vai igual em toda tentativa e serve de chave de idempotencia no provedor
    public CourierDispatch solicitarEntregador(CourierRequest requisicao) {
        for (int tentativa = 1; tentativa <= tentativas; tentativa++) {
            try {
                return chamarComTimeout(requisicao);
            } catch (TimeoutException | ExecutionException | IllegalStateException e) {
                log.warn("falha ao solicitar entregador para o pedido {} na tentativa {} de {}: {}",
                        requisicao.orderId(), tentativa, tentativas, e.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            if (tentativa < tentativas && !aguardar(tentativa)) {
                break;
            }
        }
        // o pedido ja foi pago, entao a entrega e criada aguardando entregador em vez de desfazer o pagamento
        return new CourierDispatch(null, SEM_ENTREGADOR, 0);
    }

    private CourierDispatch chamarComTimeout(CourierRequest requisicao)
            throws TimeoutException, ExecutionException, InterruptedException {
        Future<CourierDispatch> chamada = executor.submit(() -> courierClient.solicitarEntregador(requisicao));
        try {
            CourierDispatch despacho = chamada.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (despacho == null || despacho.providerStatus() == null) {
                throw new IllegalStateException("resposta invalida do provedor de entregadores");
            }
            return despacho;
        } finally {
            chamada.cancel(true);
        }
    }

    private boolean aguardar(int tentativa) {
        try {
            Thread.sleep(intervalo.multipliedBy(tentativa));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
