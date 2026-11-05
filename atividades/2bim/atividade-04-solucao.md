# Atividade 04 — 2BIM: Comunicação Assíncrona e Eventos no FoodNow

## 1. Integrante

**Nome:** Guilherme Augusto Eidam

**Ra** 23167609-2

**Modalidade:** Individual

## 2. Identificação e modelagem

A operação escolhida foi a atualização do status de uma entrega para `EM_ROTA`, realizada pelo serviço `EntregaService`.

Quando essa operação é concluída, uma ação secundária precisa ser executada: o envio de uma notificação por e-mail ao cliente informando que o pedido saiu para entrega.

A operação principal é considerada concluída após a confirmação da transação de atualização da entrega e do pedido no banco de dados.

### Fluxo atual

1. A API recebe a solicitação de atualização do status da entrega.
2. O `EntregaService` atualiza a entrega para `EM_ROTA`.
3. O pedido é atualizado para o estado correspondente.
4. O `EntregaService` chama diretamente o `NotificacaoService`.
5. O serviço de notificações envia o e-mail por meio do `FakeEmailClient`.

Nesse fluxo, a operação principal possui dependência direta do processamento da notificação.

### Fluxo proposto

1. A API recebe a solicitação de atualização do status da entrega.
2. O `EntregaService` atualiza a entrega e o pedido.
3. O serviço publica o evento `EntregaIniciadaEvent`.
4. Após a confirmação da transação, o `EntregaIniciadaListener` recebe o evento.
5. O consumidor executa o envio do e-mail de forma assíncrona pelo `FakeEmailClient`.
6. Caso ocorra uma falha, o consumidor registra o erro no log, sem desfazer a operação principal já confirmada.

### Componentes envolvidos

- **Produtor:** `EntregaService`.
- **Evento:** `EntregaIniciadaEvent`.
- **Consumidor:** `EntregaIniciadaListener`.
- **Integração simulada:** `FakeEmailClient`.

O evento transporta o identificador da entrega, o identificador do pedido e o endereço de e-mail do destinatário.

## 3. Implementação

Foi implementada uma solução de eventos locais utilizando os recursos do Spring Boot.

As principais tecnologias e anotações utilizadas foram:

- `ApplicationEventPublisher`: responsável pela publicação do evento.
- `@EnableAsync`: habilita o processamento assíncrono.
- `@Async`: executa o consumidor em outra thread.
- `@TransactionalEventListener`: permite receber eventos vinculados a uma transação.
- `TransactionPhase.AFTER_COMMIT`: garante que o consumidor seja acionado somente após a confirmação da transação.

O `EntregaService` deixou de chamar diretamente o `NotificacaoService` durante a inicialização da entrega. Agora, ele publica o evento após atualizar e salvar os dados da operação principal.

O consumidor possui uma única responsabilidade: enviar a notificação de início da entrega.

As exceções de execução do consumidor são tratadas e registradas no log, evitando que uma falha no envio do e-mail altere o resultado da operação principal já concluída.

## 4. Testes automatizados

Foi criada a classe `EntregaEventoIT`, utilizando JUnit, RestAssured e Mockito.

O teste automatizado simula uma falha no serviço de envio de e-mails e verifica os seguintes comportamentos:

1. Criação de um pedido válido.
2. Confirmação do pedido e aprovação do pagamento.
3. Inicialização da entrega com o status `EM_ROTA`.
4. Simulação de uma falha no envio da notificação.
5. Verificação de que o consumidor assíncrono foi executado.
6. Consulta posterior para confirmar que a entrega continua com o status `EM_ROTA`.

O teste demonstrou que a falha no processamento secundário não desfaz a operação de negócio já confirmada.

Os testes de API existentes foram preservados.

## 5. Análise arquitetural

### Qual dependência entre componentes foi reduzida com a publicação do evento?

A dependência direta entre `EntregaService` e `NotificacaoService` foi removida.

Anteriormente, o serviço de entrega precisava conhecer e chamar diretamente o serviço de notificações. Com a publicação do evento, o produtor passa a depender de `ApplicationEventPublisher`, enquanto o consumidor fica responsável pelo envio do e-mail.

Essa separação reduz o acoplamento entre as responsabilidades e facilita a evolução dos componentes de forma independente.

### O que pode acontecer se a aplicação encerrar depois da publicação, mas antes de o consumidor processar o evento?

Como a solução utiliza eventos locais em memória, não existe garantia de entrega durável.

Se a aplicação for encerrada depois da publicação do evento e antes do processamento pelo consumidor, a notificação poderá ser perdida.

O evento não possui armazenamento persistente, mecanismo automático de reprocessamento ou garantia de entrega após uma reinicialização.

A operação principal, entretanto, já poderá estar confirmada no banco de dados.

### Se a solução evoluir para um broker, fila ou Publish/Subscribe seria mais adequado ao fluxo escolhido? Justifique.

Para o fluxo atual, uma fila seria mais adequada, pois existe uma única ação secundária principal: enviar uma notificação por e-mail quando a entrega for iniciada.

Uma fila permitiria distribuir o processamento entre consumidores, armazenar mensagens e implementar mecanismos de novas tentativas e tratamento de mensagens que falharam.

O modelo Publish/Subscribe seria interessante caso o FoodNow passasse a possuir diversos consumidores independentes para o mesmo evento, como notificações por e-mail, SMS, push e serviços de análise.

## 6. Principais arquivos alterados

- `src/main/java/br/edu/foodnow/service/EntregaService.java`: substituição da chamada direta ao serviço de notificações pela publicação do evento.
- `src/main/java/br/edu/foodnow/config/AsyncEventConfig.java`: habilitação do processamento assíncrono.
- `src/main/java/br/edu/foodnow/event/EntregaIniciadaEvent.java`: definição do evento de início da entrega.
- `src/main/java/br/edu/foodnow/listener/EntregaIniciadaListener.java`: implementação do consumidor assíncrono e tratamento de falhas.
- `src/test/java/br/edu/foodnow/controller/EntregaEventoIT.java`: teste de integração para simular falha no envio da notificação.

## 7. Validação

Foi executado o comando:

`mvn clean verify`

**Resultado: BUILD SUCCESS.**

Resultados apresentados na execução:

- Testes unitários: 29 executados, sem falhas ou erros.
- Testes de integração: 12 executados, sem falhas ou erros.
- Total: 41 testes aprovados.
- Cobertura: todos os limites configurados no JaCoCo foram atendidos.

A validação foi concluída em 5 de novembro de 2026.

## 8. Conclusão

A implementação demonstra como a comunicação assíncrona por eventos locais pode reduzir o acoplamento entre componentes do FoodNow.

A utilização de `@Async` e `@TransactionalEventListener` permite executar a notificação após a confirmação da operação principal, preservando o resultado da atualização da entrega mesmo diante de falhas no processamento secundário.

A solução atende ao escopo da atividade sem exigir infraestrutura externa de mensageria e mantém os contratos públicos da API existentes.