# Atividade 03 - 2BIM · Integrações externas e tratamento de falhas

## Integrantes

Bryan Charles e Guilherme Sartori

## Integração escolhida

Escolhemos o provedor de entregadores, simulado pelo `FakeCourierClient`. Ele é chamado pelo `EntregaService.criarPara`, que por sua vez é chamado no fim do `PagamentoService.processar` quando o pagamento é aprovado. Essa chamada acontece dentro da mesma transação do pagamento, depois que o gateway já autorizou a cobrança.

Antes da mudança o FoodNow dependia desse serviço de forma direta e síncrona. Não existia limite de tempo, nenhuma nova tentativa e nenhum tratamento de erro. Qualquer exceção do provedor subia até o controller e qualquer lentidão segurava a requisição do cliente pelo tempo que o provedor quisesse.

## Cenários de falha analisados

**Provedor fora do ar ou devolvendo erro.** O `ApiExceptionHandler` transforma `IllegalStateException` em 422 e as demais exceções viram 500. Nos dois casos a transação é desfeita, então o pedido volta para `CONFIRMADO` e o pagamento não é gravado. Só que o gateway já tinha autorizado a transação `FN-{pedido}`, e o cliente recebe erro de um pagamento que na prática foi cobrado. Confirmamos isso rodando o teste novo contra o código original, que devolveu 422 no `POST /pagamentos`.

**Provedor lento.** Sem timeout, o `POST /pagamentos` fica preso esperando o provedor e a conexão com o banco segue aberta durante toda a espera. Com vários pedidos ao mesmo tempo isso esgota threads e conexões e afeta endpoints que nem dependem de entregador.

**Resposta inesperada.** Uma resposta nula ou sem `providerStatus` seria gravada na entrega como se fosse válida, ou quebraria mais adiante com `NullPointerException`.

## Decisão adotada

Tratamos os três cenários com um único componente, o `DespachoEntregadorResiliente`, que envolve o `FakeCourierClient` e passa a ser usado pelo `EntregaService`.

Cada tentativa roda com timeout (2s por padrão). Falha, timeout ou resposta inválida geram uma nova tentativa, até 3 no total, com espera crescente entre elas (200ms, 400ms). Todas as tentativas mandam a mesma requisição com o mesmo `orderId`, que funciona como chave de idempotência no provedor. Se todas falharem, o componente devolve `NO_COURIER_AVAILABLE`, que já é um status que o próprio provedor usa quando não há entregador. A entrega é criada como `AGUARDANDO_ENTREGADOR` e o pagamento é mantido.

Quando o provedor responde normalmente, inclusive com `NO_COURIER_AVAILABLE`, a resposta é repassada sem mudança e sem nova tentativa. Timeout, número de tentativas e intervalo podem ser ajustados em `foodnow.courier.timeout`, `foodnow.courier.tentativas` e `foodnow.courier.intervalo`.

## Principais alterações

- `integration/DespachoEntregadorResiliente.java` com timeout, retry, validação da resposta e fallback
- `service/EntregaService.java` passa a depender do componente resiliente em vez do cliente direto
- `DespachoEntregadorResilienteTest` cobre sucesso de primeira, falha transitória seguida de sucesso, timeout em todas as tentativas, resposta inválida, ausência de retry quando o provedor diz que não há entregador e interrupção da thread
- `EntregadorIndisponivelIT` sobe a aplicação com um provedor que sempre falha e confirma que o pagamento é aprovado, o pedido fica `PAGO` e a entrega fica aguardando entregador

Nenhum contrato da API mudou e nenhum teste existente foi alterado.

## Benefício e trade-off

O benefício é que uma falha no provedor de entregadores deixa de desfazer um pagamento já autorizado. O cliente recebe a resposta correta, o pedido segue o fluxo e falhas passageiras são absorvidas pelas novas tentativas sem ninguém perceber.

O trade-off é que, no pior caso, o `POST /pagamentos` pode demorar perto de 6,6s (três timeouts mais as esperas) antes de desistir, o que é mais do que um erro imediato. Além disso, a entrega pode ficar sem entregador e hoje o projeto não tem um fluxo para tentar o despacho de novo depois, então isso depende de um processo futuro ou de ação manual. Também existe o risco de o provedor ter atribuído um entregador numa tentativa que estourou o timeout. Por isso o `orderId` é o mesmo em todas as tentativas, mas essa garantia depende do provedor real respeitar essa chave.

## Resultado do `mvn clean verify`

```
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

São 36 testes unitários e 12 de API, com 97,45% de cobertura de linhas (mínimo exigido de 80%).
