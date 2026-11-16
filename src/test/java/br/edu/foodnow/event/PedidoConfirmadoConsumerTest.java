package br.edu.foodnow.event;

import br.edu.foodnow.service.NotificacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PedidoConfirmadoConsumerTest {

    @Test
    void deveIgnorarEventoDuplicadoDepoisDoEnvioBemSucedido(CapturedOutput output) {
        NotificacaoServiceFalso notificacaoService = new NotificacaoServiceFalso(false);
        PedidoConfirmadoConsumer consumidor = new PedidoConfirmadoConsumer(notificacaoService);
        PedidoConfirmadoEvent evento = eventoDeTeste();

        consumidor.consumir(evento);
        consumidor.consumir(evento);

        assertThat(notificacaoService.quantidadeDeChamadas()).isEqualTo(1);
        assertThat(output).contains("Iniciando notificacao de confirmacao. pedidoId=123");
        assertThat(output).contains("Notificacao de confirmacao enviada. pedidoId=123");
        assertThat(output).contains("Evento duplicado ignorado na notificacao de confirmacao. pedidoId=123");
    }

    @Test
    void devePermitirNovaEntregaDepoisDeFalhaNoEnvio(CapturedOutput output) {
        NotificacaoServiceFalso notificacaoService = new NotificacaoServiceFalso(true);
        PedidoConfirmadoConsumer consumidor = new PedidoConfirmadoConsumer(notificacaoService);
        PedidoConfirmadoEvent evento = eventoDeTeste();

        consumidor.consumir(evento);
        consumidor.consumir(evento);

        assertThat(notificacaoService.quantidadeDeChamadas()).isEqualTo(2);
        assertThat(output).contains("tipoFalha=IllegalStateException");
        assertThat(output).contains("resultado=CHAVE_LIBERADA_PARA_NOVA_ENTREGA");
        assertThat(output).contains("Notificacao de confirmacao enviada. pedidoId=123");
    }

    private PedidoConfirmadoEvent eventoDeTeste() {
        return new PedidoConfirmadoEvent(123L, "cliente@foodnow.local", "CENTRAL", 25);
    }

    /**
     * Implementacao falsa do serviço para controlar as respostas do teste sem
     * depender do mecanismo de instrumentacao do Mockito/Byte Buddy.
     */
    private static final class NotificacaoServiceFalso extends NotificacaoService {
        private final AtomicInteger chamadas = new AtomicInteger();
        private final boolean falharNaPrimeiraChamada;

        private NotificacaoServiceFalso(boolean falharNaPrimeiraChamada) {
            super(null, null);
            this.falharNaPrimeiraChamada = falharNaPrimeiraChamada;
        }

        @Override
        public void notificarConfirmacao(PedidoConfirmadoEvent evento) {
            int tentativa = chamadas.incrementAndGet();
            if (falharNaPrimeiraChamada && tentativa == 1) {
                throw new IllegalStateException("Falha simulada no envio da notificacao");
            }
        }

        private int quantidadeDeChamadas() {
            return chamadas.get();
        }
    }
}
