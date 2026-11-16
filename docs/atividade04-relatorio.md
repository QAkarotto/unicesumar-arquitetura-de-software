# Atividade 04 — evento local e notificação assíncrona

## Integrantes

- Felipe de Oliveira Guimarães
- Otavio Alves Brisky
- Turma: ESOFT8S

## Operação principal e ação secundária

A operação principal é a confirmação de um pedido existente em
`PedidoService.confirmar(Long)`, exposta por `POST /pedidos/{id}/confirmar`.
A única ação secundária selecionada é enviar o e-mail de confirmação.
Notificações de pagamento e início de entrega continuam no fluxo anterior.

O modelo `Pedido.confirmar()` verifica se há itens e se o estado é `CRIADO`,
então muda o estado para `CONFIRMADO`. A aplicação salva o pedido em uma
transação. **A confirmação está concluída apenas depois do commit bem-sucedido**,
não apenas ao atribuir o estado ou chamar `save`. Se houver rollback, o envio
não deve acontecer.

Esta branch, `8sfelipe-atividade04-2bim`, usa como ponto de partida o envio
síncrono original. Não depende das alterações das atividades anteriores.

## Fluxo anterior e fluxo implementado

Antes:

```text
PedidoController.confirmar
  -> PedidoService.confirmar (@Transactional)
     -> Pedido.confirmar
     -> NotificacaoService.notificarConfirmacao(Pedido)
        -> resumo logístico e FakeEmailClient.enviar
     -> PedidoRepository.save
  -> commit -> resposta HTTP
```

O pedido e suas associações eram entregues ao serviço de notificações, e a
mesma thread aguardava o envio antes de concluir a confirmação.

Depois:

```text
PedidoController.confirmar
  -> PedidoService.confirmar (@Transactional, produtor)
     -> Pedido.confirmar -> PedidoRepository.save
     -> construir uma cópia dos dados necessários
     -> ApplicationEventPublisher.publishEvent(PedidoConfirmado)
  -> commit bem-sucedido
     -> listener AFTER_COMMIT agenda a tarefa no notificacoesExecutor
  -> resposta HTTP, sem aguardar o envio

Thread notificacao-*:
  PedidoConfirmadoListener.consumir (consumidor, @Async)
    -> NotificacaoService.notificarConfirmacao(PedidoConfirmado)
       -> FakeEmailClient.enviar
```

O registro do evento ocorre depois de confirmar e salvar, ainda dentro da
transação, conforme o mecanismo de eventos transacionais do Spring. Isso não
significa notificar antes do commit: a entrega ao consumidor está vinculada
ao `AFTER_COMMIT`. Em rollback, o evento registrado não executa essa ação.
O teste de rollback distingue publicação local de processamento secundário.

O processamento pode terminar antes ou depois da resposta HTTP, mas acontece
em outra thread e a resposta não espera a conclusão do envio.

## Evento e dados transportados

`src/main/java/br/edu/foodnow/event/PedidoConfirmado.java` é um record imutável:

| Campo | Uso |
|---|---|
| `pedidoId` (`Long`) | Referência do pedido no texto do e-mail e no log de falha |
| `destinatario` (`String`) | Endereço de e-mail do cliente no instante da confirmação |
| `regiao` (`String`) | Região usada pelo texto anterior |
| `tempoEstimadoMinutos` (`int`) | Estimativa logística usada pelo texto anterior |

Não transporta entidades JPA, itens, valores ou dados de pagamento. O consumidor
não consulta o banco, não depende de sessão JPA aberta nem precisa enxergar
entidades ainda não confirmadas. A região e a estimativa são calculadas com
`LogisticaService.resumirPedido` e copiadas durante a operação, preservando o
conteúdo da confirmação mesmo se o pedido evoluir antes do envio.

## Alterações e configuração assíncrona

Arquivos de implementação sob `src/main/java/br/edu/foodnow/`:

- `service/PedidoService.java`: substitui a dependência direta de
  `NotificacaoService` por `ApplicationEventPublisher`; produz o evento após
  confirmar e salvar, sem mudar as validações da confirmação.
- `event/PedidoConfirmado.java`: representa o fato e seus dados imutáveis.
- `event/PedidoConfirmadoListener.java`: possui
  `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`
  e `@Async("notificacoesExecutor")`; executa somente a notificação.
- `config/NotificacaoAsyncConfig.java`: habilita `@EnableAsync` e declara o
  executor com uma thread, prefixo `notificacao-` e fila de até 100 tarefas.
- `service/NotificacaoService.java`: monta a mensagem de confirmação a partir
  do evento. Remove a dependência de logística, que ficou sem uso nesse serviço;
  o envio das mensagens de pagamento e entrega não foi modificado.

`@Async` é aplicado pelo proxy Spring ao listener chamado pelo publicador;
não há chamada interna ao próprio método que contorne o proxy. O teste confirma
a identidade diferente das threads e o prefixo da thread consumidora.

`fallbackExecution = true` evita que uma publicação fora de transação seja
ignorada pelo listener. Nesse caso, agenda-se diretamente a execução assíncrona,
pois não há commit a aguardar. O fluxo de produção continua transacional; o
fallback é coberto por um teste explícito de publicação sem transação.

O executor tem fila limitada para evitar crescimento ilimitado de tarefas.
Quando está cheio ou indisponível, seu handler registra um erro de descarte.
A tarefa não é executada na thread produtora e a rejeição não é propagada
para a operação principal. No encerramento ordenado, a configuração tenta
aguardar as tarefas por até cinco segundos; isso não constitui garantia durável.

## Falhas e isolamento

O consumidor captura `RuntimeException` ao executar a ação secundária e
registra o identificador do pedido e o tipo da falha, sem expor destinatário,
conteúdo ou mensagem da exceção. Esse catch é limitado à fronteira assíncrona;
não envolve validação ou persistência da confirmação. Não há retry automático.

Falhas do consumidor, inclusive defeitos inesperados representados por
`RuntimeException`, são registradas para investigação. Não retornam à thread
HTTP nem alteram o pedido já confirmado. Erros fora dessa categoria seguem
o tratamento de exceções assíncronas do Spring e também não revertem o commit.

Dois casos são tratados e testados: falha no envio após a confirmação e fila
do executor cheia. Nos dois casos, a confirmação não passa a depender de um
envio bem-sucedido; a notificação pode ser perdida, com registro em log.

## Dependência reduzida, benefícios e limites

`PedidoService` deixa de chamar o serviço de notificações diretamente e passa
a depender do publicador e do fato `PedidoConfirmado`. O consumidor concentra
a escolha da ação secundária. A mensagem continua dependendo do resumo
logístico, mas essa dependência não obriga o consumidor a receber uma entidade.

Benefícios: a confirmação não aguarda o envio, não é desfeita por falha
secundária e não notifica pedidos cujo commit falhou. O escopo permanece
local, com Spring e o simulador existentes, sem nova infraestrutura.

Limitações e trade-offs:

- Eventos e fila vivem na memória. Se a aplicação encerrar depois do commit,
  antes do agendamento ou durante o envio, a confirmação pode permanecer sem
  e-mail. Não há reprocessamento, outbox ou armazenamento durável do evento.
- Uma falha de resposta do provedor pode deixar o resultado do envio incerto.
  A solução não garante idempotência ou entrega exatamente uma vez.
- Uma thread limita a concorrência e preserva a sequência das tarefas aceitas
  por esse executor. Não garante ordenação global com as notificações síncronas
  de pagamento/entrega. A disponibilidade imediata do e-mail ao retornar o POST
  deixa de ser uma garantia; a notificação é eventual.
- Uma ação que trave pode ocupar a thread; um adaptador real precisaria impor
  timeout de transporte. A fila pode encher e descartar tarefas com log.
- O contrato HTTP continua com os mesmos endpoints, DTOs e códigos. Os testes
  antigos de sucesso e as referências completas de geografia são preservados;
  seus resultados nesta execução não representam garantia de prazo de envio.

## Evolução eventual para broker

Para este fluxo, escolheria **fila de trabalho**: há uma única ação secundária,
e uma mensagem deve ser processada por um trabalhador de notificação. Várias
instâncias poderiam competir pelos trabalhos, com confirmação de processamento,
retry limitado e idempotência definidos explicitamente.

Publish/Subscribe seria adequado se surgissem ações independentes que
precisassem receber cada fato, como notificação, auditoria e métricas. Cada
assinante precisaria de sua própria entrega. Não é necessário adicionar esses
consumidores nesta atividade.

Um broker isolado não elimina a janela entre commit do banco e publicação.
Uma evolução com garantia durável também precisaria avaliar outbox e a
idempotência do consumidor. Nenhuma dessas garantias é atribuída ao evento local.

## Testes automatizados

Os arquivos de testes anteriores não foram alterados. Foram adicionados:

- `src/test/java/br/edu/foodnow/controller/PedidoConfirmadoEventoIT.java`:
  seis testes usando a aplicação Spring real, executor assíncrono e banco H2
  separado. Verificam publicação e conteúdo, envio em outra thread após o commit,
  confirmação persistida enquanto o consumidor está bloqueado, falha de envio
  sem rollback, rollback sem envio, publicação fora de transação e confirmação
  rejeitada sem novo evento. O primeiro teste reúne publicação e execução.
- `src/test/java/br/edu/foodnow/config/NotificacaoAsyncConfigTest.java`:
  verifica saturação da fila com descarte registrado, sem exceção ao produtor
  nem execução da tarefa rejeitada na thread chamadora.

Não se usa `Thread.sleep`. `CountDownLatch` controla a entrada e a liberação
do consumidor; uma barreira na fila com `Future.get` observa a conclusão das
tarefas anteriores. Os limites de dez segundos apenas fazem o teste falhar
se um sinal não chegar, sem definir um atraso artificial para aprová-lo.

## Resultados reais da validação

Verificação final concluída em **16/11/2026**, na branch
`8sfelipe-atividade04-2bim`, com o comando PowerShell:

```powershell
mvn --batch-mode --no-transfer-progress "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean verify
```

O parâmetro seleciona o cache de dependências do usuário e não altera a
configuração do projeto. Resultado final:

```text
Unitários: Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
API:       Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

Foram **47 execuções aprovadas**: os 40 testes anteriores desta branch e sete
testes novos. Os resultados foram conferidos nos XMLs de
`target/surefire-reports` e `target/failsafe-reports`.

Cobertura de linhas extraída de `target/site/jacoco/jacoco.xml`:

| Escopo | Linhas cobertas | Não cobertas | Cobertura |
|---|---:|---:|---:|
| Relatório geral | 726 | 13 | **98,24%** |
| Check com exclusão de `FoodNowApplication` já existente | 725 | 11 | **98,51%** |

O mínimo de **80%** foi aprovado pelo `jacoco:check`, sem alteração no
`pom.xml`, nas exclusões, no pipeline ou nas configurações existentes.
Relatório HTML: `target/site/jacoco/index.html`.
