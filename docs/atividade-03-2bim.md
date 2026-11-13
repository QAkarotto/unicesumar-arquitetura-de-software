# Atividade 03 - 2BIM: Integrações Externas e Tratamento de Falhas no FoodNow

## Integrantes

- Danilo Bolzan de Moraes
- Edi Carlos Francisco Junior

## Integração escolhida

Despacho de entregadores pelo `FakeCourierClient`, chamado por `EntregaService.criarPara`
logo após a aprovação do pagamento.

## Dependência atual e cenários de falha

Antes da mudança, `EntregaService` chamava o provedor diretamente, sem timeout,
tentativas adicionais, validação de resposta ou fallback.

Foram analisados os seguintes cenários:

1. **Indisponibilidade ou erro do provedor:** a exceção propagava para o fluxo do
   pagamento. Como o gateway já poderia ter autorizado a cobrança, desfazer a
   transação local criava inconsistência entre sistemas.
2. **Lentidão excessiva:** sem timeout, a requisição poderia permanecer bloqueada,
   mantendo recursos da aplicação ocupados por tempo indeterminado.
3. **Resposta inesperada:** retorno nulo ou sem status poderia causar erro posterior
   ou persistir dados inválidos.

## Decisão adotada

Foi criado `DespachoEntregadorResiliente`, responsável por envolver o provedor com:

- timeout configurável por tentativa, com padrão de 2 segundos;
- até 3 tentativas para erro, timeout ou resposta inválida;
- espera crescente de 200 ms e 400 ms entre as tentativas;
- reutilização da mesma requisição e do mesmo `orderId` como chave de idempotência;
- fallback `NO_COURIER_AVAILABLE` após esgotar as tentativas.

Uma resposta válida `NO_COURIER_AVAILABLE` não é repetida. Nesse caso, ou quando o
fallback é aplicado, a entrega permanece `AGUARDANDO_ENTREGADOR` sem invalidar o
pagamento aprovado.

As propriedades `foodnow.courier.timeout`, `foodnow.courier.tentativas` e
`foodnow.courier.intervalo` permitem ajustar a política sem alterar código.

## Principais alterações

- componente resiliente para chamadas ao provedor de entregadores;
- `EntregaService` passou a utilizar esse componente;
- testes unitários para sucesso, retry, timeout, resposta inválida e ausência de
  retry em resposta válida sem entregador;
- teste de integração comprovando que a falha externa não desfaz o pagamento;
- nenhum endpoint ou teste de API existente foi alterado.

## Benefício e trade-off

O benefício é absorver falhas transitórias e preservar a consistência do pagamento
quando o serviço de entregadores estiver indisponível. O `orderId` constante também
permite que um provedor real implemente idempotência e evite despachos duplicados.

O trade-off é o aumento de latência no pior caso: com os valores padrão, as três
tentativas podem consumir aproximadamente 6,6 segundos. Além disso, o fallback deixa
a entrega aguardando entregador e ainda exige um processo posterior de novo despacho.
A idempotência depende de o provedor real respeitar o `orderId` recebido.

## Verificação

O comando `mvn clean verify` foi executado com sucesso:

- 34 testes unitários aprovados;
- 12 testes de integração/API aprovados;
- nenhuma falha, erro ou teste ignorado;
- 97,05% de cobertura de linhas, acima do mínimo de 80%.
