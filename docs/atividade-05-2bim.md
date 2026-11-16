# Atividade 05 – Resiliência e Observabilidade no FoodNow

## Identificação

- Integrantes: Danilo Bolzan de Moraes e Edi Carlos Francisco Junior.
- Fluxo: autorização de pagamento e envio do e-mail de resultado.
- Componente: `PagamentoService`, que chama o provedor de pagamento e depois `NotificacaoService`.
- Comportamento esperado: após o provedor autorizar, o pedido passa para `PAGO`, o pagamento fica `APROVADO`, a entrega é criada e o cliente é avisado por e-mail.

## Falha simulada e impacto

No fluxo anterior, depois de o provedor responder que o pagamento foi autorizado, o serviço gravava pedido e pagamento e enviava o e-mail antes do commit da transação. Se o cliente de e-mail falhasse, a exceção podia reverter os registros locais mesmo com a autorização já ocorrida no provedor. Isso deixava o estado local divergente do externo.

O teste `PagamentoNotificacaoResilienciaIT` simula a indisponibilidade do cliente de e-mail (`FakeEmailClient` lança `IllegalStateException`) após a autorização. Executado contra o código **sem a correção**, o teste falha:

```
Expected status code <201> but was <422>.
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
BUILD FAILURE
```

Impacto observado no código anterior:

- O cliente recebia `422 REGRA_NEGOCIO`, como se o pagamento tivesse sido recusado, embora o provedor já tivesse autorizado a cobrança.
- A transação era revertida: pagamento, mudança do pedido para `PAGO` e criação da entrega se perdiam.
- Uma nova tentativa do cliente geraria uma segunda autorização no provedor (cobrança duplicada).
- A mensagem interna da exceção era devolvida no corpo da resposta, e nenhum log identificava pedido ou tipo de falha.

## Melhoria e limites

A chamada de notificação agora é isolada em um tratamento localizado (`notificarSemComprometerPagamento`). A falha é registrada e não escapa do serviço; assim, o commit da operação principal não é desfeito por um efeito secundário. O log não inclui o token de pagamento, o destinatário nem a mensagem da exceção, apenas o nome da classe da falha.

Justificativa: o e-mail é uma consequência do pagamento, não parte dele. A falha de uma integração secundária não deve invalidar uma operação financeira já confirmada pelo provedor externo. Contratos da API e o fluxo de sucesso não mudaram.

Não há repetição automática: o sistema não sabe se uma integração real aceitou a mensagem antes de ocorrer o erro, então repetir poderia enviar duplicado. A limitação é que o e-mail pode não ser entregue e não há fila durável ou mecanismo de reprocessamento; o log de erro serve de base para reenvio manual. Uma evolução seria um outbox transacional com reenvio idempotente.

## Evidências e validação

- Teste automatizado: `PagamentoNotificacaoResilienciaIT.falhaNaNotificacaoNaoDesfazPagamentoERegistraDiagnostico`.
- O teste simula `IllegalStateException`, confirma que a API responde `201` com pagamento `APROVADO`, que pagamento e pedido (`PAGO`) continuam consultáveis, que o e-mail foi tentado e confere os campos de diagnóstico no log. Também verifica que o token enviado na requisição e a mensagem da exceção não aparecem no log.

Registro gerado durante o teste:

```
ERROR --- b.edu.foodnow.service.PagamentoService : Falha na notificação de pagamento pedidoId=1 pagamentoId=1 status=APROVADO tipoFalha=IllegalStateException resultado=NAO_ENVIADA; pagamento preservado
```

No caminho de sucesso é registrado em nível `INFO`: `Notificação de pagamento processada pedidoId=... pagamentoId=... status=... resultado=ENVIADA`. Nos testes esse registro não aparece porque `src/test/resources/application.properties` define `logging.level.root=WARN`.

### Resultado de `mvn clean verify`

**Resultado: sucesso.** Executado `mvn -Dmaven.repo.local=/private/tmp/foodnow-tools/m2cache clean verify` com Apache Maven 3.9.16 e Eclipse Temurin JDK 26.0.2.1. Passaram os 45 testes (29 unitários e 16 de integração; 0 falhas, 0 erros, 0 ignorados), e a checagem JaCoCo de cobertura mínima foi aprovada. Nenhum teste existente foi alterado.

```
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

## Reflexão

A solução melhora os dois aspectos: torna o pagamento mais resistente ao erro do e-mail, pois preserva a operação já autorizada, e facilita o diagnóstico ao registrar a operação afetada, o tipo da falha e o resultado do tratamento. A entrega do e-mail em si não é garantida.
