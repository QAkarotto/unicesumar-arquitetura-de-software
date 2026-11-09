# Atividade 05 - 2BIM · Resiliência e observabilidade

## Integrantes

Bryan Charles e Guilherme Sartori

## Fluxo escolhido

Escolhemos o pagamento do pedido, feito pelo `POST /pagamentos` e tratado no `PagamentoService.processar`. Ele depende de uma integração externa, o provedor de pagamento simulado pelo `FakePaymentGateway`.

O comportamento esperado é simples. O pedido precisa estar `CONFIRMADO`, o provedor autoriza ou recusa a cobrança, o pedido passa para `PAGO` ou `PAGAMENTO_REJEITADO`, o pagamento fica gravado com o código da transação e, se aprovado, a entrega é criada. O cliente deve ser cobrado uma única vez por pedido.

## Cenário de falha

Simulamos três falhas do provedor, trocando o `FakePaymentGateway` por uma versão instável só no teste.

1. Indisponibilidade antes da cobrança. O provedor recusa a conexão e nada é cobrado.
2. Resposta perdida depois da cobrança. O provedor autoriza e grava a transação, mas a resposta não chega e o cliente HTTP estoura por tempo. É o caso mais perigoso, porque a cobrança existe e o FoodNow não sabe disso.
3. Falha também na consulta ao provedor, quando nem dá para saber se a cobrança aconteceu.

## Comportamento atual e impacto

Rodamos o teste novo contra o código original. Os quatro casos falharam com `Expected status code <201> but was <500>` ou `<503> but was <500>`.

O `PagamentoService` chamava o provedor uma única vez e não tratava exceção nenhuma. Qualquer falha virava um 500 genérico do Spring e a transação era desfeita, então o pedido voltava para `CONFIRMADO` sem pagamento gravado.

No cenário da resposta perdida isso é grave. O provedor já tinha cobrado, o FoodNow não registrou nada e o pedido continuava pagável. Quando o cliente tentasse de novo, o `POST /pagamentos` passava pela validação e mandava uma segunda cobrança ao provedor. Uma falha passageira também não tinha segunda chance e derrubava o pagamento na hora.

O diagnóstico também era ruim. O único registro era o stack trace do `dispatcherServlet`, sem o pedido, sem a tentativa e sem dizer o que foi feito depois. E como o stack trace leva a mensagem da exceção do provedor, o token de pagamento apareceu 8 vezes no log da execução.

## Melhoria implementada

Criamos o `integration.GatewayPagamentoResiliente`, que fica entre o `PagamentoService` e o provedor. A única mudança no serviço foi trocar o tipo da dependência. A ideia é "consultar antes de repetir".

Quando a chamada falha, ele primeiro consulta o provedor pelo código da transação do pedido (`FN-{pedidoId}`), usando o `consultarPagamento` que o gateway já tinha. Se a transação existe, a cobrança aconteceu e só a resposta se perdeu, então ele devolve esse resultado e o fluxo segue normal, sem cobrar de novo. Se não existe, ele tenta de novo, até 3 tentativas no total, com espera crescente de 200ms e 400ms. Se a própria consulta falhar, ele para na hora, porque sem saber se houve cobrança repetir poderia duplicar o pagamento.

Quando não tem como concluir, ele lança `PagamentoIndisponivelException`, que o `ApiExceptionHandler` transforma em 503 `PAGAMENTO_INDISPONIVEL`, no mesmo formato de erro das outras respostas. A transação é desfeita e o pedido continua `CONFIRMADO`, então o cliente pode tentar depois. O teste mostra que a tentativa seguinte, com o provedor de volta, aprova o pagamento com uma única cobrança.

Para não duplicar a regra do código da transação, o `FakePaymentGateway` ganhou o método estático `codigoTransacao`, usado por ele e pelo componente novo.

## Justificativa

Retry sozinho seria perigoso aqui. Cobrança não é uma operação que pode ser repetida sem cuidado, e o caso da resposta perdida é justamente quando repetir cobra duas vezes. Por isso a repetição só acontece quando o provedor confirma que não há transação para o pedido. A referência do pedido funciona como chave de idempotência, já que o provedor gera sempre o mesmo código para o mesmo pedido.

A solução fica no adaptador da integração, que é onde está o conhecimento sobre o provedor. O `PagamentoService` continua com a regra de negócio e não sabe nada de tentativas. Isso segue o mesmo caminho da Atividade 03 com o entregador.

Trocar o 500 por 503 não muda o contrato dos fluxos que funcionam. O 500 era um erro não tratado, e o 503 diz ao cliente que o problema é temporário e que vale tentar de novo.

## Limitações

A espera entre as tentativas acontece dentro da transação e da thread da requisição. No pior caso a resposta demora uns 600ms a mais e a conexão com o banco fica presa nesse tempo.

Não colocamos timeout próprio. Assumimos que o cliente HTTP do provedor real já lança exceção quando estoura o tempo, como no cenário simulado.

A consulta só acontece depois de uma falha na mesma requisição. Se a aplicação cair entre a cobrança e o commit, a próxima requisição não consulta antes de cobrar. Num provedor real o certo seria mandar uma chave de idempotência na própria cobrança, para o provedor recusar a duplicata do lado dele.

Se o provedor recusar o pagamento e a resposta se perder, a consulta devolve a recusa, e o pedido vira `PAGAMENTO_REJEITADO` como seria sem a falha.

## Registros de diagnóstico

Cada linha leva a operação, o pedido, a tentativa, o tipo da falha e o resultado do tratamento, no formato chave=valor para facilitar a busca. Falha de uma tentativa sai em WARN e desfecho sem sucesso em ERROR. O log registra só o nome da classe da exceção e nunca a mensagem, porque a mensagem do provedor pode trazer dados do pagamento. O token não aparece nem uma vez no log da suíte nova, e o teste confere isso com `doesNotContain`.

Registros gerados pela execução do `GatewayPagamentoInstavelIT`

```
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=1 tentativa=1/3 falha=ProvedorIndisponivelException
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=1 tentativa=2/3 falha=ProvedorIndisponivelException
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=1 tentativa=3/3 resultado=recuperado status=AUTHORIZED
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=2 tentativa=1/3 falha=ProvedorIndisponivelException
ERROR GatewayPagamentoResiliente : operacao=consultar_pagamento pedido=2 transacao=FN-2 falha=ProvedorIndisponivelException resultado=interrompido_estado_desconhecido
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=3 tentativa=1/3 falha=ProvedorTimeoutException
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=3 resultado=reconciliado transacao=FN-3 status=AUTHORIZED
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=4 tentativa=1/3 falha=ProvedorIndisponivelException
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=4 tentativa=2/3 falha=ProvedorIndisponivelException
WARN  GatewayPagamentoResiliente : operacao=processar_pagamento pedido=4 tentativa=3/3 falha=ProvedorIndisponivelException
ERROR GatewayPagamentoResiliente : operacao=processar_pagamento pedido=4 resultado=tentativas_esgotadas tentativas=3
```

## Testes

O `GatewayPagamentoInstavelIT` estende o `ApiTestSupport` e troca o provedor por uma versão instável via `@TestConfiguration` com `@Primary`. Ela segue um roteiro de falhas e conta quantas chamadas e quantas cobranças de fato aconteceram. Os logs são capturados com o `OutputCaptureExtension`.

- `respostaPerdidaDepoisDaCobrancaDeveReconciliarSemCobrarDeNovo` devolve 201 `APROVADO`, o pedido fica `PAGO` e há uma única cobrança
- `indisponibilidadeTemporariaDeveSerSuperadaComNovaTentativa` aprova na terceira chamada com uma cobrança
- `indisponibilidadeContinuaDeveResponder503EManterOPedidoPagavel` devolve 503, não cobra, mantém o pedido `CONFIRMADO` e aprova na tentativa seguinte do cliente
- `falhaNaConsultaNaoDeveRepetirACobranca` devolve 503 depois de uma única chamada

Nenhum teste existente foi alterado.

## Resultado da suíte

`mvn clean verify` com JDK 26 terminou em BUILD SUCCESS, com 29 testes unitários e 15 de API, todos passando, e cobertura de 97,63% das linhas, acima do mínimo de 80% do JaCoCo.

## Reflexão

Ambos. O sistema fica mais resistente porque uma falha passageira não derruba o pagamento e, principalmente, porque a resposta perdida deixa de virar cobrança em dobro. E fica mais fácil de diagnosticar porque antes havia só um stack trace genérico sem o pedido e com o token exposto, e agora dá para seguir cada pagamento pelo número do pedido, ver quantas tentativas foram feitas, o tipo de cada falha e como terminou. O caso `interrompido_estado_desconhecido` é o melhor exemplo dos dois juntos. O sistema escolhe não arriscar uma cobrança duplicada e deixa registrado exatamente qual pedido precisa ser conferido com o provedor.
