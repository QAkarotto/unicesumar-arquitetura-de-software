# Atividade 04 - 2º Bim
Comunicação Assíncrona e Eventos no FoodNow

## Identificação e modelagem

- A operação escolhida foi a **Confirmação de Pedido** e a ação secundaria é a **Notificação de um Pedido Confirmado**
- **Fluxo Atual:** PedidoService -> ConfirmaPedido -> RegistraNotificação
- **Fluxo Proposto:** PedidoService -> ConfirmaPedido -> PedidoConfirmadoEvent -> NotificacaoPedidoListener -> RegistraNotificaçãoEmSegundoPlano
- Foi criado o **PedidoConfirmadoEvent** que representa que o pedido já foi confirmado. 

## Implementação

- Criação do evento que irá identificar o pedido e informar os dados principais do pedido

```
public record PedidoConfirmadoEvent(
        Long pedidoId,
        String clienteEmail,
        String regiao,
        int tempoMinutos
)
```

- Habilitar o processamento assíncrono 
```
package br.edu.foodnow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FoodNowApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodNowApplication.class, args);
    }
}
```

- Criar a classe que irá ouvir o evento e executar a ação de enviar a notificação, alem disso vai tratar se houver falhas 
```
@Component
public class NotificacaoPedidoListener {
    private static final Logger log =
            LoggerFactory.getLogger(NotificacaoPedidoListener.class);
    private final FakeEmailClient emailClient;
    public NotificacaoPedidoListener(FakeEmailClient emailClient) {
        this.emailClient = emailClient;
    }
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmarPedido(PedidoConfirmadoEvent evento) {
        try {
            String mensagem = "O pedido " + evento.pedidoId()
                    + " foi confirmado para a região "
                    + evento.regiao()
                    + ". Estimativa interna: "
                    + evento.tempoMinutos() + " minutos.";
            emailClient.enviar(
                    evento.clienteEmail(),
                    "Pedido confirmado",
                    mensagem
            );
        } catch (RuntimeException ex) {
            log.error(
                    "Falha ao enviar notificação do pedido {}",
                    evento.pedidoId(),
                    ex
            );
        }
    }
}
```
## Análise da solução
- A dependência direta entre PedidoService e NotificacaoService foi reduzida, antes o serviço de pedidos era responsevel por confirmar pedido e chamar diretamente o serviço de notificações. Com o evento o pedidoService só informa que o pedido foi confirmado. 
- Como a solução utiliza eventos locais do Spring o evento existe apenas em memória. Se a aplicação encerrar o evento poderá ser pedido e a notificação não será enviada
- Para a solução escolhida a fila seria mais adequada, pois o evento PedidoConfirmado aciona uma tarefa especifica e seria possivel implementar tentativas de reenvio em caso de falha.