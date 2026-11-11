# Atividade 05 — 2BIM: Resiliência e Observabilidade no FoodNow

## 1. Integrante

**Nome:** Guilherme Augusto Eidam

**RA:** 23167609-2

**Modalidade:** Individual

## 2. Fluxo escolhido

O fluxo escolhido foi o processamento de pagamentos e o envio de notificações por e-mail.

Os componentes envolvidos são:

- `PagamentoService`: responsável pelo processamento do pagamento.
- `NotificacaoService`: responsável pela preparação e pelo envio da notificação.
- `FakePaymentGateway`: integração simulada de pagamento.
- `FakeEmailClient`: cliente simulado de envio de e-mails.

## 3. Cenário de falha e impacto identificado

Na implementação original, o método `PagamentoService.processar()` realizava o processamento do pagamento, registrava o resultado e chamava diretamente o serviço de notificações dentro da mesma transação.

Um cenário relevante ocorre quando o provedor de pagamento aprova a operação, mas o serviço de e-mail apresenta uma falha durante o envio da notificação.

Nesse cenário, uma exceção não tratada poderia interromper a transação local, mesmo depois da aprovação da operação pelo serviço externo.

O impacto seria a inconsistência entre o resultado obtido no provedor e o registro local do pagamento.

## 4. Melhoria de resiliência implementada

Foi implementado um tratamento localizado no `PagamentoService` para impedir que uma falha no envio da notificação interrompa o processamento principal do pagamento.

O envio foi isolado em um método privado chamado `notificarSemComprometerPagamento()`.

Esse método executa a notificação dentro de um bloco `try/catch`, capturando exceções do tipo `RuntimeException`.

Quando o envio é concluído, um registro de sucesso é gerado. Quando ocorre uma falha, o erro é registrado e o processamento principal continua.

Não foram adicionadas novas tentativas automáticas de envio, pois uma repetição sem controle de idempotência pode produzir notificações duplicadas.

A melhoria preserva os contratos existentes da API e o comportamento dos fluxos de sucesso.

## 5. Observabilidade e registros de diagnóstico

Foram adicionados registros utilizando SLF4J.

As informações registradas incluem:

- Identificador do pedido (`pedidoId`).
- Identificador do pagamento (`pagamentoId`).
- Status do pagamento.
- Tipo da exceção ocorrida.
- Resultado do tratamento, indicando envio concluído ou notificação não enviada.

Os registros não incluem tokens de pagamento ou outros dados sensíveis.

Em caso de falha, a mensagem de diagnóstico indica que o pagamento foi preservado.

## 6. Teste automatizado

Foi criada a classe `PagamentoResilienciaIT`.

O teste utiliza JUnit, Mockito, RestAssured e `OutputCaptureExtension` para reproduzir e verificar o comportamento esperado.

O cenário executado foi:

1. Criar um pedido válido.
2. Confirmar o pedido.
3. Simular uma falha no envio da notificação de pagamento.
4. Processar um pagamento utilizando o gateway simulado.
5. Verificar que a API respondeu com HTTP 201 e status `APROVADO`.
6. Consultar o pagamento e verificar que ele permaneceu aprovado.
7. Consultar o pedido e verificar que seu status permaneceu `PAGO`.
8. Verificar o registro de diagnóstico com os identificadores, tipo de falha e resultado do tratamento.

O teste confirmou que a falha no envio do e-mail não impediu a conclusão do pagamento.

## 7. Justificativa arquitetural e limitações

A melhoria reduz o impacto de uma falha secundária sobre uma operação de negócio importante.

O pagamento não depende mais do sucesso do envio do e-mail para continuar seu processamento local.

Como limitação, a solução não implementa uma fila persistente, novas tentativas ou reprocessamento automático. Portanto, uma notificação que falhar poderá não ser entregue.

Também não há garantia de idempotência para o envio do e-mail. A decisão de não repetir automaticamente evita introduzir um risco adicional de notificações duplicadas.

## 8. Questão para reflexão

**A solução torna o sistema mais resistente à falha, mais fácil de diagnosticar ou ambos?**

A solução contribui para ambos.

Ela aumenta a resiliência porque impede que uma falha no serviço de notificações interrompa o processamento local do pagamento.

Também melhora a observabilidade porque os registros permitem identificar qual pedido e pagamento foram afetados, qual tipo de exceção ocorreu e qual foi o resultado do tratamento.

Assim, a equipe consegue compreender melhor o problema e investigar a ocorrência sem comprometer a operação principal.

## 9. Validação

Foi executado o comando:

`mvn clean verify`

**Resultado: BUILD SUCCESS.**

- Testes unitários: 29 executados, sem falhas ou erros.
- Testes de integração: 12 executados, sem falhas ou erros.
- Total: 41 testes aprovados.
- Cobertura: todos os limites configurados no JaCoCo foram atendidos.

A validação foi concluída em 11 de novembro de 2026.

## 10. Conclusão

A implementação demonstra que o tratamento de falhas e a geração de registros de diagnóstico são importantes para a confiabilidade do FoodNow.

A solução adotada protege o processamento do pagamento contra falhas no envio de notificações e fornece informações suficientes para facilitar a identificação e a análise dessas ocorrências, sem alterar os contratos públicos existentes.