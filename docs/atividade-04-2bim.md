# Atividade 04 – Comunicação Assíncrona e Eventos no FoodNow

## Identificação

- Integrantes: Danilo Bolzan de Moraes e Edi Carlos Francisco Junior

## Operação escolhida

A confirmação de um pedido é a operação principal. Ela é considerada concluída quando o status `CONFIRMADO` é salvo e a transação do serviço faz commit. O envio de e-mail simulado ao cliente é a única ação secundária movida para processamento assíncrono.

### Fluxo atual

`PedidoController → PedidoService.confirmar → muda o status → envia e-mail sincronamente → responde`

Se o envio falhasse, a exceção ocorria dentro da transação e poderia impedir a confirmação.

### Fluxo proposto

`PedidoController → PedidoService.confirmar (produtor) → salva status e publica PedidoConfirmado → commit → NotificacaoPedidoConfirmadoListener (consumidor em outra thread) → FakeEmailClient`

O evento contém `pedidoId`, `emailCliente`, `regiao` e `estimativaMinutos`. Assim o consumidor não consulta entidades JPA nem depende de dados que ainda não foram confirmados. O listener roda depois do commit, com `@TransactionalEventListener(AFTER_COMMIT)` e `@Async("eventExecutor")`; falhas no envio são registradas em log e não alteram a confirmação.

## Teste e validação

`ConfirmacaoAssincronaIT` simula uma falha no cliente de e-mail, verifica que o consumidor tentou enviar a notificação e confirma que a API respondeu com `CONFIRMADO` e que esse status continua consultável.

Como o e-mail de confirmação passou a ser enviado em outra thread, a ordem dele em relação aos e-mails que continuam síncronos (por exemplo, o de pagamento) deixa de ser garantida. O `GeografiaContratoIT` compara a sequência de e-mails com um arquivo de referência que espera "Pedido confirmado" na primeira posição. O teste continua passando, porque o consumidor termina antes da requisição de pagamento seguinte, mas essa ordem agora depende de tempo de execução. O teste não foi alterado, conforme as restrições da atividade.

### Resultado de `mvn clean verify`

**Resultado: sucesso.** Executado `mvn -Dmaven.repo.local=/private/tmp/foodnow-tools/m2cache clean verify` com Apache Maven 3.9.16 e Eclipse Temurin JDK 26.0.2.1. Passaram os 44 testes (29 unitários e 15 de integração; 0 falhas, 0 erros, 0 ignorados), e a checagem JaCoCo de cobertura mínima foi aprovada. As ferramentas e o repositório Maven ficaram temporariamente em `/private/tmp/foodnow-tools`; a instalação não altera as ferramentas globais da máquina.

## Análise para o Pull Request

- **Dependência reduzida:** `PedidoService` deixa de depender diretamente de `NotificacaoService` para enviar e-mail. O produtor conhece o evento de negócio; o consumidor conhece a integração de e-mail. A confirmação não espera a integração.
- **Encerramento entre publicação e consumo:** como o evento existe somente em memória, a aplicação pode encerrar antes do processamento e perder a notificação. Não há persistência, reentrega nem garantia de processamento exatamente uma vez.
- **Fila ou Publish/Subscribe:** Publish/Subscribe é mais adequado a esse fato, pois a confirmação pode interessar a outros consumidores independentes no futuro (por exemplo, auditoria ou atualização de métricas). Com apenas um consumidor atualmente, uma fila também funcionaria, mas comunica melhor distribuição de trabalho entre consumidores concorrentes do mesmo tipo. A escolha de Publish/Subscribe favorece a expansão para múltiplas reações independentes.


