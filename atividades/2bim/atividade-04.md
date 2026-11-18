# Atividade 04 – Comunicação Assíncrona e Eventos no FoodNow

**Integrante:** Ramon Albini Vieira

## 1. Operação escolhida

Foi escolhida a confirmação de pedidos.

A operação principal consiste em alterar o status do pedido de CRIADO para CONFIRMADO e persistir essa alteração no banco de dados.

A ação secundária é enviar uma notificação por e-mail ao cliente informando a confirmação.

## 2. Fluxo atual

O `PedidoService` confirma o pedido e chama diretamente o `NotificacaoService`, que utiliza o `FakeEmailClient` para enviar a notificação.

O envio acontece durante a execução da operação principal, criando dependência direta entre a confirmação e o processamento da notificação.

## 3. Fluxo proposto

1. O `PedidoService` confirma e salva o pedido.
2. O produtor publica `PedidoConfirmadoEvent`, contendo o identificador do pedido.
3. O `PedidoConfirmadoListener` recebe o evento após o commit da transação.
4. O consumidor executa em outra thread utilizando `@Async`.
5. O consumidor recupera o pedido persistido e utiliza `NotificacaoService` para enviar a notificação pelo `FakeEmailClient`.
6. Falhas na tarefa secundária são registradas em log, sem desfazer o pedido confirmado.

## 4. Dependência reduzida

A publicação do evento reduz o acoplamento entre `PedidoService` e `NotificacaoService`.

O produtor não precisa conhecer o consumidor nem os detalhes da integração de e-mail. Ele apenas publica o fato de que um pedido foi confirmado.

Isso permite adicionar ou substituir consumidores sem alterar diretamente o fluxo de confirmação.

## 5. Risco de encerramento da aplicação

Como a solução utiliza eventos locais em memória, existe o risco de a aplicação encerrar após o commit e antes de o consumidor concluir o processamento.

Nesse cenário, o evento pode ser perdido e a notificação não será enviada.

A solução não oferece entrega durável, reprocessamento automático ou garantia de processamento após reinicialização.

Essas limitações são aceitas no escopo desta atividade, que não exige a implementação de broker ou infraestrutura adicional.

## 6. Evolução para um broker

Para o fluxo escolhido, uma fila seria mais adequada inicialmente.

Existe uma única ação secundária: enviar a notificação de confirmação. Uma fila permitiria que um consumidor responsável pelo envio processasse cada mensagem, desacoplando ainda mais o produtor da infraestrutura de notificação.

Publish/Subscribe seria mais interessante caso outros componentes precisassem reagir independentemente ao mesmo evento, como auditoria, analytics e atualização de outros sistemas.

## 7. Validação

- Testes de execução do consumidor assíncrono.
- Teste de falha na notificação sem desfazer a confirmação.
- Preservação do fluxo de pedidos existente.
- Cobertura JaCoCo: 97,19%