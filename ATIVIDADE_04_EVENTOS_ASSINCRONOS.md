# Atividade 04 — Comunicação Assíncrona e Eventos no FoodNow

## Integrante
- Samuel Vitor da Cruz Silva

## Operação escolhida e conclusão do fluxo principal

A operação escolhida foi a confirmação de um pedido, realizada por `PedidoService.confirmar`. A operação principal é considerada concluída quando a alteração do status para `CONFIRMADO` é persistida e a transação do método é efetivamente commitada.

A ação secundária escolhida é o envio da notificação de confirmação ao cliente por meio da integração simulada `FakeEmailClient`.

## Fluxo atual

```text
PedidoController
    -> PedidoService.confirmar
        -> Pedido.confirmar (status = CONFIRMADO)
        -> NotificacaoService.notificarConfirmacao
            -> FakeEmailClient.enviar
        -> PedidoRepository.save
    -> resposta HTTP
```

No fluxo original, o envio da notificação acontecia diretamente dentro da chamada de confirmação e antes do retorno do método transacional. Uma falha no envio podia propagar uma exceção e afetar a transação da operação principal.

## Fluxo proposto

```text
PedidoController
    -> PedidoService.confirmar
        -> Pedido.confirmar (status = CONFIRMADO)
        -> PedidoRepository.save
        -> ApplicationEventPublisher.publishEvent
            -> PedidoConfirmadoEvent
    -> commit da transação
        -> PedidoConfirmadoConsumer
            (@TransactionalEventListener: AFTER_COMMIT)
            (@Async: foodNowEventExecutor)
            -> NotificacaoService.notificarConfirmacao
                -> FakeEmailClient.enviar
```

**Produtor:** `PedidoService.confirmar`.

**Evento:** `PedidoConfirmadoEvent`, contendo o identificador do pedido, o e-mail do cliente, a região de entrega e o tempo estimado. O evento carrega os dados necessários para a notificação, sem exigir que o consumidor consulte uma entidade ainda não confirmada.

**Consumidor:** `PedidoConfirmadoConsumer`, responsável exclusivamente por solicitar o envio da notificação de confirmação.

## Implementação e tratamento de falhas

O produtor publica o evento depois de salvar o pedido, ainda dentro da transação. O consumidor utiliza `@TransactionalEventListener(phase = AFTER_COMMIT)`, portanto só recebe o evento quando a transação que confirmou o pedido é commitada. A anotação `@Async`, habilitada por `@EnableAsync`, executa o consumidor em um executor dedicado (`foodNowEventExecutor`), sem manter o envio de e-mail no caminho síncrono da requisição.

O consumidor captura exceções de execução do envio e registra a ocorrência no log. Dessa forma, uma falha da notificação não é propagada para a operação de confirmação que já foi commitada.

## Cenários de falha considerados

1. **Falha na integração de e-mail:** o cliente de e-mail simulado pode lançar uma exceção durante o envio. O consumidor registra o erro; o pedido permanece com status `CONFIRMADO`.
2. **Encerramento da aplicação antes do processamento:** a aplicação pode encerrar depois do commit e antes de o executor processar o evento. Como o evento é local e mantido apenas em memória, ele pode ser perdido.

## Testes automatizados

- `FoodNowFlowIT` continua validando o fluxo da API e aguarda a notificação assíncrona, sem alterar endpoints ou contratos públicos.
- `PedidoConfirmacaoFalhaIT` substitui o serviço de notificação por uma implementação de teste que simula uma exceção no consumidor. O teste confirma que o consumidor foi executado, que a requisição de confirmação retornou sucesso e que o pedido continua com status `CONFIRMADO`.

## Análise da solução

**Qual dependência entre componentes foi reduzida?**

`PedidoService` deixou de chamar diretamente `NotificacaoService` para enviar a notificação. O serviço de negócio conhece o evento que representa o fato ocorrido, enquanto o consumidor conhece a tarefa secundária de notificar. Isso reduz o acoplamento temporal e direto entre a confirmação do pedido e a integração de e-mail.

**O que pode acontecer se a aplicação encerrar depois da publicação, mas antes do processamento?**

O evento pode ser perdido, pois o mecanismo local do Spring não oferece armazenamento durável. Não há garantia de reentrega, reprocessamento ou idempotência nesta solução.

**Se a solução evoluir para um broker, fila ou Publish/Subscribe seria mais adequado?**

Para o fluxo selecionado, que possui um único consumidor responsável pelo envio da notificação, uma fila seria a opção inicial mais adequada: a mensagem seria destinada ao processamento dessa tarefa por um consumidor lógico. Publish/Subscribe seria mais interessante se vários consumidores independentes precisassem reagir ao mesmo fato, por exemplo, notificações, auditoria e análise de dados.

## Benefício e trade-off

O principal benefício é retirar o envio de e-mail do caminho síncrono da confirmação, reduzindo o acoplamento e evitando que uma falha do consumidor desfaça uma operação já commitada. O trade-off é que a notificação passa a ter consistência eventual e pode ser perdida se a aplicação for encerrada antes do processamento ou se o consumidor falhar; nesta atividade não foram adicionadas garantias de persistência ou reprocessamento.

## Resultado de validação

O comando `mvn clean verify` foi executado no ambiente local com JDK 26 e terminou com **BUILD SUCCESS** em 2 minutos e 19 segundos, em 06/11/2026.

- Testes unitários (Surefire): 32 executados, 0 falhas, 0 erros e 0 ignorados.
- Testes de integração (Failsafe): 12 executados, 0 falhas, 0 erros e 0 ignorados.
- JaCoCo: todas as verificações de cobertura configuradas no projeto foram atendidas.

Durante `PedidoConfirmacaoFalhaIT`, a falha de envio foi simulada intencionalmente e registrada pelo consumidor no log. O próprio teste terminou com sucesso, confirmando que a falha do processamento secundário não desfez a confirmação do pedido. Também foram exibidos avisos do Mockito/Byte Buddy e de API depreciada; eles não impediram a execução bem-sucedida da validação.
