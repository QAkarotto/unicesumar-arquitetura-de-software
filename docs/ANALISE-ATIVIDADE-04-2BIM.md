# Atividade 04 - 2BIM · Comunicação assíncrona e eventos

## Integrantes

Bryan Charles e Guilherme Sartori

## Operação escolhida

Escolhemos a confirmação do pedido, feita pelo `POST /pedidos/{id}/confirmar` e tratada no `PedidoService.confirmar`. A operação principal está concluída quando o pedido passa para `CONFIRMADO` e essa mudança é gravada no banco, ou seja, quando a transação faz commit.

O envio do e-mail "Pedido confirmado" é uma tarefa secundária. Ele não muda nada no pedido e o cliente não precisa esperar por ele para receber a resposta. Mesmo assim, no código original ele rodava dentro da mesma transação e antes do `save`. Uma falha no e-mail desfazia a confirmação e o cliente recebia erro de um pedido que estava pronto para ser confirmado.

## Fluxo atual

```
Cliente -> PedidoController -> PedidoService.confirmar  (transação aberta)
                                 pedido.confirmar()
                                 NotificacaoService.notificarConfirmacao  (mesma thread, mesma transação)
                                   LogisticaService.resumirPedido
                                   FakeEmailClient.enviar
                                 pedidoRepository.save
                               commit
Cliente <- 200 CONFIRMADO  (só depois do e-mail)
```

## Fluxo proposto

```
Cliente -> PedidoController -> PedidoService.confirmar  (produtor, transação aberta)
                                 pedido.confirmar()
                                 pedidoRepository.save
                                 publica PedidoConfirmado
                               commit
Cliente <- 200 CONFIRMADO

        depois do commit, na thread foodnow-eventos-N
        NotificacaoPedidoConfirmadoListener  (consumidor)
          carrega o pedido já gravado
          NotificacaoService.notificarConfirmacao
          falha fica registrada no log e não volta para o pedido
```

O produtor é o `PedidoService`, o evento é o `PedidoConfirmado` e o consumidor é o `NotificacaoPedidoConfirmadoListener`.

## O evento

O `PedidoConfirmado` é um record imutável que descreve um fato que já aconteceu. Ele leva o `pedidoId`, o `clienteId`, o `restauranteId`, o `valorTotal` e o instante da confirmação. Não leva a entidade `Pedido`, porque ela pertence à transação do produtor e não deve ser usada em outra thread. O consumidor usa o `pedidoId` para carregar o pedido numa transação própria de leitura, que só começa depois do commit, então ele sempre enxerga a confirmação já gravada.

## Implementação

O `PedidoService.confirmar` agora grava o pedido e publica o evento pelo `ApplicationEventPublisher`. Ele deixou de depender do `NotificacaoService`.

O consumidor usa `@TransactionalEventListener(phase = AFTER_COMMIT)`, que só entrega o evento se a transação da confirmação fizer commit. Se a confirmação falhar e for desfeita, nenhum e-mail sai. Junto dele vai o `@Async`, para rodar fora da thread da requisição, e o `@Transactional(propagation = REQUIRES_NEW, readOnly = true)`, para abrir uma transação nova só de leitura e poder navegar pelas associações do pedido.

O processamento assíncrono foi ligado com `@EnableAsync` no `config.AsyncConfig`, que também define o executor `taskExecutor` com threads nomeadas `foodnow-eventos-`, de 2 a 4 threads e fila de 100 tarefas. No desligamento normal ele espera até 10s as tarefas que estiverem na fila.

Qualquer exceção do envio é capturada no consumidor e registrada no log com o id do pedido. Ela não sobe para lugar nenhum e o pedido continua `CONFIRMADO`.

## Principais alterações

- `evento/PedidoConfirmado.java` com o evento
- `evento/NotificacaoPedidoConfirmadoListener.java` com o consumidor
- `config/AsyncConfig.java` com o `@EnableAsync` e o executor
- `service/PedidoService.java` publica o evento no lugar de chamar a notificação
- `PedidoConfirmadoEventoIT` sobe a aplicação e confirma um pedido pela API. O primeiro cenário verifica que o consumidor rodou numa thread `foodnow-eventos-`, recebeu o pedido certo e enviou o e-mail. O segundo faz a notificação lançar exceção e verifica que o pedido continua `CONFIRMADO` e que o pagamento é aprovado normalmente depois
- `NotificacaoPedidoConfirmadoListenerTest` cobre o consumidor isolado com envio normal, falha no envio e pedido inexistente

Nenhum endpoint foi criado, nenhum contrato mudou e nenhum teste existente foi alterado. Os testes de API que conferem os e-mails, inclusive o de contrato geográfico que compara a ordem das mensagens, continuam passando sem mudança.

## Análise da solução

**Qual dependência entre componentes foi reduzida com a publicação do evento?**

O `PedidoService` dependia diretamente do `NotificacaoService` e, por ele, do `FakeEmailClient` e do cálculo de resumo da logística feito só para montar o texto do e-mail. Agora ele só conhece o evento e o `ApplicationEventPublisher`. Ele não sabe quem consome a confirmação nem quantos consumidores existem. A dependência passou a ir do consumidor para o evento, e não mais do serviço de pedidos para a notificação. Isso também tirou o acoplamento temporal, porque a resposta ao cliente e a transação da confirmação não esperam mais o e-mail.

**O que pode acontecer se a aplicação encerrar depois da publicação, mas antes de o consumidor processar o evento?**

O evento existe só na memória, dentro da fila do executor. Se a aplicação cair nesse intervalo, o pedido fica `CONFIRMADO` no banco e o e-mail nunca é enviado. Nada registra que faltou essa notificação, então ninguém fica sabendo e não tem como reprocessar. No desligamento normal o executor espera até 10s pelas tarefas pendentes, o que diminui a chance, mas não resolve queda abrupta, `kill -9` ou falta de energia. O mesmo vale para uma falha no envio, que hoje só deixa um registro no log. Para ter garantia de entrega seria preciso gravar o evento no banco na mesma transação do pedido (padrão outbox) e ter um processo que publique e reprocesse a partir dessa tabela, com consumidores idempotentes.

**Se a solução evoluir para um broker, fila ou Publish/Subscribe seria mais adequado ao fluxo escolhido? Justifique.**

Publish/Subscribe. O `PedidoConfirmado` é um fato do domínio e não um comando para um destinatário específico. Hoje só a notificação reage a ele, mas é natural que outras partes queiram saber da confirmação, como o restaurante começar o preparo, a logística reservar capacidade ou um relatório de vendas. Com um tópico, cada uma dessas partes assina o evento de forma independente e recebe sua própria cópia, sem que o `PedidoService` precise mudar. Uma fila simples entrega cada mensagem para um único consumidor e faria sentido se o fluxo fosse um comando como "envie este e-mail". Na prática cada assinante do tópico pode ter várias instâncias dividindo o trabalho, como num grupo de consumidores, então a fila aparece dentro de cada assinatura e não no lugar do tópico.

## Trade-off

O cliente recebe a resposta da confirmação sem esperar o e-mail, e uma falha no envio não derruba mais a confirmação. Em troca, quando o `200` volta não há garantia de que o e-mail já saiu. Em teoria o e-mail de confirmação pode até chegar depois do e-mail de pagamento se as duas requisições forem muito próximas. Nos testes isso não aconteceu em três execuções seguidas do `mvn clean verify`, porque o envio leva poucos milissegundos e o pagamento é bem mais demorado. Também ficou mais difícil de testar, já que o teste precisa esperar o consumidor terminar em vez de conferir o resultado logo depois da chamada.

## Resultado do `mvn clean verify`

```
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

São 32 testes unitários e 13 de API, com 97,97% de cobertura de linhas (mínimo exigido de 80%).
