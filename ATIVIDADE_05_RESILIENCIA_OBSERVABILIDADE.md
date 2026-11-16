# Atividade 05 — Resiliência e Observabilidade no FoodNow

## Integrante
- Samuel Vitor da Cruz Silva

## Fluxo escolhido

Foi selecionado o fluxo de confirmação de pedido e envio assíncrono da notificação ao cliente, implementado na Atividade 04.

- **Produtor:** `PedidoService.confirmar`.
- **Evento:** `PedidoConfirmadoEvent`.
- **Componente responsável pelo consumo:** `PedidoConfirmadoConsumer`.
- **Ação secundária:** envio da notificação por `NotificacaoService`, utilizando a integração simulada `FakeEmailClient`.

O consumidor continua sendo acionado somente após o commit da transação, por meio de `@TransactionalEventListener(phase = AFTER_COMMIT)`, e executado no executor assíncrono já configurado.

## Cenário de falha e impacto

O cenário simulado é a entrega duplicada do mesmo `PedidoConfirmadoEvent`, com o mesmo identificador de pedido. Na implementação anterior, cada chamada ao consumidor executava novamente `NotificacaoService.notificarConfirmacao`. Assim, a repetição do evento poderia enviar mais de uma notificação de confirmação para o mesmo pedido.

O impacto é a duplicidade de mensagens para o cliente e a execução repetida de uma ação secundária que deveria ocorrer uma única vez por pedido confirmado.

## Melhoria implementada

Foi adicionada uma proteção localizada no `PedidoConfirmadoConsumer`, baseada no identificador do pedido (`pedidoId`). O consumidor mantém um conjunto concorrente de identificadores já processados com sucesso, utilizando `ConcurrentHashMap.newKeySet()`.

1. Ao receber o evento, o consumidor tenta incluir o `pedidoId` no conjunto. A inclusão é atômica: se o identificador já estiver presente, o evento é reconhecido como duplicado, registrado como aviso e ignorado.
2. Na primeira entrega, o consumidor registra o início e executa a notificação.
3. Em caso de sucesso, registra a conclusão e mantém o identificador no conjunto, bloqueando novas execuções para o mesmo pedido durante a vida da instância.
4. Se ocorrer uma `RuntimeException`, remove o identificador do conjunto e registra o tipo da falha e que a chave foi liberada para uma nova entrega do evento.

A falha continua isolada da confirmação do pedido, que já foi commitada. Os registros contêm a operação, o identificador do pedido, o tipo de falha e o resultado do tratamento. Não são registrados e-mail, token, senha ou conteúdo da notificação.

## Justificativa arquitetural e limitações

A melhoria reduz o risco de efeitos duplicados e facilita o diagnóstico do processamento assíncrono, sem alterar endpoints, contratos de API ou o fluxo bem-sucedido de confirmação. A estrutura concorrente evita que duas chamadas simultâneas na mesma instância assumam o mesmo pedido ao mesmo tempo.

A solução é intencionalmente local e em memória. Portanto:

- os identificadores são perdidos quando a aplicação é reiniciada;
- instâncias diferentes da aplicação não compartilham o conjunto;
- o conjunto cresce conforme novos pedidos são processados;
- uma nova entrega só será tentada se o evento for entregue novamente; a implementação não agenda automaticamente uma repetição;
- se o provedor externo efetivar o envio, mas a aplicação receber uma exceção antes de confirmar o resultado, uma nova entrega poderá causar duplicidade.

Assim, a implementação reduz duplicidades dentro de uma instância em execução, mas não oferece garantia de execução exatamente uma vez nem substitui uma estratégia persistente de idempotência.

## Testes automatizados

Foi criado `PedidoConfirmadoConsumerTest` com dois cenários. O teste utiliza uma implementação falsa (`NotificacaoServiceFalso`) para controlar as respostas e contar as chamadas, sem depender do mecanismo de instrumentação Mockito/Byte Buddy:

- `deveIgnorarEventoDuplicadoDepoisDoEnvioBemSucedido`: entrega o mesmo evento duas vezes e verifica que o serviço de notificação foi chamado somente uma vez. Também verifica os registros de início, sucesso e duplicidade.
- `devePermitirNovaEntregaDepoisDeFalhaNoEnvio`: simula uma falha na primeira tentativa e verifica que uma segunda entrega pode executar o serviço novamente. Também verifica o registro do tipo de falha e da liberação da chave.

Os testes exercitam diretamente a lógica do consumidor; os testes de integração existentes continuam responsáveis por verificar o fluxo transacional e assíncrono da aplicação. Os testes existentes não foram alterados.

## Registros de diagnóstico verificados pelos testes

Para um evento processado com sucesso e depois recebido novamente, o teste verifica mensagens com este conteúdo:

```text
Iniciando notificacao de confirmacao. pedidoId=123
Notificacao de confirmacao enviada. pedidoId=123
Evento duplicado ignorado na notificacao de confirmacao. pedidoId=123
```

Para uma falha seguida de nova entrega, o teste verifica:

```text
Falha na notificacao de confirmacao. pedidoId=123, tipoFalha=IllegalStateException, resultado=CHAVE_LIBERADA_PARA_NOVA_ENTREGA
```

As linhas acima mostram o conteúdo esperado das mensagens; o `OutputCaptureExtension` faz as asserções sobre os registros efetivamente emitidos durante os testes. Anexar ao Pull Request uma captura da execução local dos testes.

## Resultado da validação

**Pendente de nova execução no ambiente local após a correção dos testes.** A execução compartilhada anteriormente terminou com `BUILD FAILURE` por falha na inicialização do Mockito/Byte Buddy nos dois testes novos. Os testes foram alterados para usar um serviço falso manual, sem Mockito. Executar novamente na raiz do projeto, na mesma pasta em que está este relatório e o `pom.xml`:

```bash
mvn clean verify
```

Registrar nesta seção o resultado real do Maven, a quantidade de testes executados, eventuais falhas/erros e a confirmação da cobertura JaCoCo. Não considerar a validação concluída antes de obter `BUILD SUCCESS`.

## Reflexão

A solução contribui para os dois atributos. Ela aumenta a resiliência ao ignorar entregas duplicadas e ao liberar a chave quando o envio falha, permitindo uma nova entrega. Também melhora a observabilidade ao registrar o início, o sucesso, a duplicidade e a falha com o identificador da operação e o resultado do tratamento. As garantias permanecem limitadas à memória de uma única instância em execução.
