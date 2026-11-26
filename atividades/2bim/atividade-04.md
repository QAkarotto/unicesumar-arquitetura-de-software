# Atividade 04 – Comunicação Assíncrona e Eventos no FoodNow

## Contexto

Uma operação do FoodNow pode desencadear uma ação secundária, como enviar uma notificação após a confirmação de um pedido. A execução dessa ação não deve atrasar nem invalidar uma operação de negócio já concluída.

Nesta atividade, implemente uma solução com **eventos locais e processamento assíncrono no Spring Boot**. Não é necessário instalar Kafka, RabbitMQ ou outro broker de mensagens.

## Atividade

### 1. Identificação e modelagem

1. Escolha uma operação já existente no FoodNow que possa desencadear uma ação secundária.
2. Identifique quando a operação principal é considerada concluída e qual tarefa pode ser executada separadamente.
3. Represente brevemente o fluxo atual e o fluxo proposto, identificando o produtor, o evento e o consumidor.
4. Defina um evento que represente um fato ocorrido, por exemplo, `PedidoConfirmado`, com os dados necessários para o processamento.

### 2. Implementação

1. Publique o evento após a conclusão bem-sucedida da operação principal.
2. Implemente um consumidor responsável por **uma única ação secundária**, utilizando uma integração simulada ou um registro de log, conforme o projeto.
3. Configure o processamento assíncrono. No Spring, `@EventListener` não executa automaticamente em outra thread; utilize `@Async` com a configuração necessária ou abordagem equivalente.
4. Trate falhas do consumidor sem alterar o resultado da operação principal já concluída.
5. Caso haja transação de banco de dados, evite que o consumidor dependa de informações ainda não confirmadas. Considere `@TransactionalEventListener` com processamento após o commit.

### 3. Validação

1. Preserve os endpoints e o comportamento da operação principal.
2. Crie ou adapte **pelo menos um teste automatizado** que verifique a publicação do evento ou a execução do consumidor.
3. Simule uma falha no processamento secundário e verifique que ela não desfaz a operação principal concluída.
4. Execute `mvn clean verify` e registre o resultado.

### 4. Análise da solução

Responda no Pull Request:

- Qual dependência entre componentes foi reduzida com a publicação do evento?
- O que pode acontecer se a aplicação encerrar depois da publicação, mas antes de o consumidor processar o evento?
- Se a solução evoluir para um broker, **fila ou Publish/Subscribe** seria mais adequado ao fluxo escolhido? Justifique.

## Restrições

- Não crie endpoints nem altere contratos públicos existentes.
- Não altere testes de API para acomodar mudanças indevidas no comportamento esperado.
- Limite a implementação ao fluxo selecionado, sem refatorações extensas.
- Não implemente infraestrutura externa de mensageria.
- Considere que eventos locais assíncronos, isoladamente, **não garantem entrega durável, reprocessamento ou idempotência**. Essas garantias não precisam ser implementadas nesta atividade.

## Entrega

Abra um **Pull Request** individualmente ou em dupla, identificando os integrantes. Inclua a implementação, o teste automatizado, o resultado de `mvn clean verify`, os dois fluxos e as respostas da análise.
