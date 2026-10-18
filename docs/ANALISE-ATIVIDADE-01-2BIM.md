# Atividade 01 do 2º bimestre (FoodNow, APIs como fronteiras arquiteturais)

Bryan Charles e Guilherme Sartori

## Base e verificação inicial

A branch saiu do `master` atual, que já traz a base refatorada do professor (PR #72). A nossa
Atividade 06 ainda não está no `master`, e partir dela faria o diff deste PR misturar as duas
entregas. Como a refatoração não mexeu no contrato de pedidos, as duas bases são equivalentes
para esta atividade.

Antes de qualquer alteração, com JDK 26 (Temurin 26.0.2) e Maven 3.9.9.

```
mvn clean verify  ->  BUILD SUCCESS
Testes unitários  29, falhas 0
Testes de API     11, falhas 0
All coverage checks have been met.
```

| Contador | Coberto/Total | Percentual |
|---|---|---|
| Linhas | 700/715 | 97,90% |
| Branches | 107/132 | 81,06% |
| Instruções | 3738/3867 | 96,66% |

## 1. Contrato atual das operações de pedidos

O contrato foi levantado lendo `PedidoController`, `ApiDtos` e `ApiExceptionHandler` e depois
conferido chamando a aplicação empacotada, porque alguns comportamentos só aparecem na prática.

| Operação | Parâmetros | Corpo | Sucesso | Erros |
|---|---|---|---|---|
| `POST /pedidos` | nenhum | `PedidoRequest` | 201 `PedidoResponse` | 400, 404, 422 |
| `GET /pedidos/{id}` | `id` no caminho, int64 | nenhum | 200 `PedidoResponse` | 404, 422 |
| `POST /pedidos/{id}/itens` | `id` no caminho, int64 | `ItemPedidoRequest` | 200 `PedidoResponse` | 400, 404, 422 |
| `POST /pedidos/{id}/confirmar` | `id` no caminho, int64 | nenhum | 200 `PedidoResponse` | 404, 422 |

### DTOs envolvidos

| DTO | Campos | Validações |
|---|---|---|
| `PedidoRequest` | `clienteId`, `restauranteId`, `enderecoEntregaId`, `itens` | os três ids `@NotNull`, `itens` `@NotEmpty` com cada item `@Valid` |
| `ItemPedidoRequest` | `produtoId`, `quantidade` | `produtoId` `@NotNull`, `quantidade` `@Min(1)` |
| `PedidoResponse` | `id`, `clienteId`, `restauranteId`, `enderecoEntregaId`, `itens`, `subtotal`, `taxaEntrega`, `valorTotal`, `distanciaEntregaKm`, `status` | resposta |
| `ItemResponse` | `produtoId`, `nome`, `quantidade`, `precoUnitario`, `subtotal` | resposta |
| `ErroResponse` | `status`, `erro`, `mensagem` | resposta de erro do handler |

As quatro operações devolvem o mesmo `PedidoResponse`, então qualquer campo novo nele aparece
nas quatro de uma vez.

### Erros por origem

| Status | `erro` | Quando acontece |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Bean Validation falhou. A mensagem traz só o primeiro campo, no formato `campo: mensagem` |
| 400 | corpo padrão do Spring | corpo ausente ou JSON malformado, que não chega ao `ApiExceptionHandler` |
| 404 | `NOT_FOUND` | cliente, restaurante, produto ou pedido inexistente |
| 422 | `REGRA_NEGOCIO` | endereço que não é do cliente, fora da área, produto indisponível, de outro restaurante ou que não pode ser entregue, e confirmação de pedido que não está em `CRIADO` |

### Comportamentos que só apareceram chamando a API

Campos desconhecidos no corpo são ignorados. Um cliente que mandasse `observacao` antes desta
atividade recebia 201 e o campo era descartado em silêncio.

JSON malformado devolve `{"timestamp","status","error","path"}` e não o `ErroResponse`. É um
segundo formato de erro 400 que os clientes precisam aceitar.

Um id não numérico, como `GET /pedidos/abc`, devolve 422 `REGRA_NEGOCIO` e não 400. A falha de
conversão do Spring é uma `IllegalArgumentException` e cai no handler de regra de negócio.

`POST /pedidos/{id}/itens` não confere o status, então aceita itens em pedido já confirmado. A
taxa de entrega calculada na criação também não é recalculada ao adicionar itens.

As mensagens de validação vêm em inglês, do Hibernate Validator no locale padrão, por exemplo
`clienteId: must not be null`.

O `application.properties` de teste substitui o principal por inteiro, então a opção
`spring.jackson.default-property-inclusion=non_null` vale em produção e não vale nos testes.
Para o campo novo isso importa, e por isso a regra de omissão foi colocada no próprio DTO.

### Testes que já cobriam essas operações

| Teste | O que protege |
|---|---|
| `FoodNowFlowIT` | fluxo completo de criar, adicionar item, confirmar, pagar e entregar, com status, subtotal e total |
| `FoodNowErrorsIT` | 422 de produto indisponível e de fora da área, 400 de pedido sem itens, pagamento rejeitado |
| `GeografiaContratoIT` | compara o JSON inteiro das quatro operações em quatro cenários contra snapshots em `src/test/resources/geografia` |
| `ApiTestSupport` | monta o cenário e cria pedido pelo `POST /pedidos` |
| `PedidoTest` | regras de domínio de itens, valores, confirmação e pagamento |

O `GeografiaContratoIT` é o consumidor mais exigente do repositório, porque falha se aparecer
qualquer chave nova no JSON de pedido. Ele pesou na decisão sobre o formato da resposta.

## 2. Especificação OpenAPI do contrato atual

O projeto não tinha especificação. A primeira versão está em `docs/openapi/foodnow-pedidos.yaml`,
OpenAPI 3.1, e foi commitada sozinha antes de qualquer mudança no código (commit `6647bf3`, versão
`1.0.0` do contrato). Ela descreve as quatro operações com parâmetros, request bodies, schemas,
respostas de sucesso e de erro, incluindo o corpo padrão do Spring para JSON malformado, e traz
exemplos de request, de response e de erro.

## 3. Proposta da evolução

### Campo e DTOs afetados

O campo é `observacao`, do tipo string, opcional e com no máximo 500 caracteres.

| Onde | Mudança |
|---|---|
| `PedidoRequest` | ganha `observacao` com `@Size(max = 500)` |
| `PedidoResponse` | ganha `observacao`, omitido do JSON quando não existe |
| `Pedido` | ganha o atributo `observacao`, coluna `varchar(500)`, definido só no construtor |
| `ItemPedidoRequest`, `ItemResponse`, `ErroResponse` | sem mudança |

Não existe setter nem endpoint de edição. A observação só entra pelo `POST /pedidos`.

### Tratamento das entradas

| Entrada em `observacao` | Resultado | Status | Campo na resposta |
|---|---|---|---|
| ausente | pedido sem observação | 201 | chave ausente |
| `null` | pedido sem observação | 201 | chave ausente |
| `""` | pedido sem observação | 201 | chave ausente |
| só espaços ou quebras de linha | pedido sem observação | 201 | chave ausente |
| texto de 1 a 500 caracteres | gravado como enviado, sem trim | 201 | texto |
| texto com 501 ou mais | rejeitado, nada é gravado | 400 `VALIDATION_ERROR` | não se aplica |

Texto em branco vira ausência porque o campo é opcional e um texto em branco não carrega
informação nenhuma para o restaurante. Rejeitar com 400 puniria clientes que mandam string
vazia por padrão de formulário, que é bem comum.

O texto não leva trim para guardar exatamente o que o cliente escreveu. O limite é conferido
sobre o texto enviado, então 500 espaços passam e viram ausência, e 501 espaços são rejeitados.
É um caso de borda sem uso real e manter uma regra só deixa a validação mais simples de explicar.

O limite excedido usa 400 e não 422 porque é validação de formato do corpo, igual às outras
validações de Bean Validation da API. A mensagem segue o padrão atual,
`observacao: size must be between 0 and 500`.

Quando não há observação a chave é omitida em vez de vir como `null`. Assim o JSON de um pedido
sem observação continua idêntico ao da versão anterior, byte a byte, e consumidores estritos
como o `GeografiaContratoIT` não percebem diferença. A omissão está em `@JsonInclude(NON_NULL)`
no próprio componente do record, para não depender da configuração global que hoje só vale em
produção.

O `Pedido` repete a regra de texto em branco e de limite. A API valida primeiro, mas a entidade
fica protegida se surgir outra porta de entrada no futuro.

O limite conta posições de `String.length` do Java. Um emoji fora do plano básico ocupa duas
posições, o que está anotado na especificação.

### Consumidores potencialmente afetados

Encontrados no repositório

| Consumidor | Como usa | Efeito |
|---|---|---|
| `PedidoController` e `ApiDtos` | produzem o contrato | alterados nesta atividade |
| `FoodNowFlowIT`, `FoodNowErrorsIT`, `ApiTestSupport` | criam e leem pedidos sem observação e conferem campos pontuais | nenhum |
| `GeografiaContratoIT` | compara o JSON inteiro de pedido | nenhum, porque pedidos sem observação não ganham chave nova |
| `PagamentoService`, `EntregaService`, `NotificacaoService` | leem a entidade `Pedido`, não o DTO | nenhum, não usam a observação |
| `PagamentoResponse` e `EntregaResponse` | só expõem `pedidoId` | nenhum |

Não existe frontend, SDK ou cliente gerado dentro do repositório.

Hipotéticos

| Consumidor | Risco |
|---|---|
| app do cliente que cria pedidos | nenhum enquanto não enviar o campo. Se já mandava `observacao` com mais de 500 caracteres, que antes era ignorada, passa a receber 400 |
| painel do restaurante que lê pedidos | precisa ser tolerante a campo desconhecido para ler pedidos com observação |
| integração de parceiro ou BI com schema estrito, por exemplo `additionalProperties: false` ou Jackson com `FAIL_ON_UNKNOWN_PROPERTIES` | quebra ao receber um pedido com observação criado por outro cliente |
| cliente gerado a partir de uma especificação antiga | ignora o campo ou falha, conforme o gerador |
| teste de contrato de terceiro que compara o JSON inteiro | quebra só para pedidos com observação |

### Classificação

A mudança é compatível, por ser aditiva, desde que valham estas condições.

1. O campo é opcional na entrada e nenhum request que funcionava antes passa a falhar.
2. Nenhum campo é removido, renomeado ou muda de tipo, e nenhum status novo aparece para os
   requests antigos.
3. Pedidos sem observação continuam com o mesmo JSON, sem a chave nova.
4. Quem lê pedidos com observação ignora campos desconhecidos, no estilo tolerant reader.

A condição 4 é a única que não depende do servidor. Um consumidor estrito que leia um pedido com
observação quebra, e para ele a mudança seria incompatível. Também existe a borda do cliente que
já mandava um campo chamado `observacao` com mais de 500 caracteres, que antes era ignorado e
agora gera 400. Os dois casos são hipotéticos e não existem no repositório.

### Versionamento

Não criamos `/v2`. As rotas continuam as mesmas e a especificação passa de `1.0.0` para `1.1.0`,
seguindo a ideia do versionamento semântico, onde mudança aditiva sobe a versão menor.

Uma `/v2` duplicaria as quatro operações para proteger só consumidores estritos hipotéticos, e
ainda teria que decidir o que a v1 mostra quando lê um pedido criado com observação pela v2. O
custo não se paga enquanto a mudança é aditiva. Uma nova versão passaria a fazer sentido se a
observação ficasse obrigatória, se mudasse de nome ou de tipo, se passasse a aceitar edição com
outro formato, ou se aparecesse um consumidor estrito real que não pudesse ser atualizado.

## 4. Implementação

| Arquivo | Mudança |
|---|---|
| `controller/ApiDtos.java` | `observacao` em `PedidoRequest` com `@Size(max = Pedido.LIMITE_OBSERVACAO)` e em `PedidoResponse` com `@JsonInclude(NON_NULL)` |
| `controller/PedidoController.java` | repassa `request.observacao()` para o service |
| `service/PedidoService.java` | `criar` recebe a observação e passa para o construtor de `Pedido` |
| `model/Pedido.java` | constante `LIMITE_OBSERVACAO`, atributo com `@Column(length = 500)`, construtor novo que normaliza a observação e `getObservacao` |
| `docs/openapi/foodnow-pedidos.yaml` | versão `1.1.0` com o campo, os exemplos e o erro de limite |

O construtor antigo de `Pedido`, sem observação, continua existindo e delega para o novo com
`null`. Itens, valores, taxa, pagamento e entrega não foram tocados.

## 5. Comparação entre os contratos

O diff completo está no histórico.

```
git diff 6647bf3 0e9d6cf -- docs/openapi/foodnow-pedidos.yaml
```

| Elemento | 1.0.0 | 1.1.0 |
|---|---|---|
| Rotas e métodos | 4 operações | as mesmas 4 |
| `PedidoRequest` | 4 campos obrigatórios | os mesmos 4 obrigatórios e `observacao` opcional, string ou null, até 500 |
| `PedidoResponse` | 10 campos obrigatórios | os mesmos 10 obrigatórios e `observacao` opcional, de 1 a 500 caracteres, omitido quando não existe |
| `ItemPedidoRequest` e `ItemResponse` | sem observação | sem mudança |
| `POST /pedidos` sucesso | 201 | 201 |
| `POST /pedidos` erros | 400, 404, 422 | os mesmos, e o 400 passa a cobrir também `observacao` acima de 500 |
| `GET`, `itens`, `confirmar` | 200 com `PedidoResponse` | 200 com `PedidoResponse`, que agora pode trazer `observacao` |
| Edição da observação | não existe | continua sem existir, o campo é ignorado em `POST /pedidos/{id}/itens` |
| Exemplos | pedido sem observação | pedido sem e com observação, erro de limite |

## 6. Testes

Os testes existentes não foram alterados. O diff da pasta de testes só tem linhas adicionadas,
e os snapshots do `GeografiaContratoIT` passam iguais, o que confirma que pedidos sem observação
saem com o mesmo JSON de antes.

Novos testes de API em `ObservacaoPedidoIT`

| Teste | O que cobre |
|---|---|
| `deveManterObservacaoEmTodasAsOperacoesDoPedido` | observação informada na criação, recuperada no `GET`, mantida em `itens`, `confirmar` e depois do pagamento, gravada no banco, com subtotal inalterado |
| `deveCriarPedidoSemObservacaoSemExporOCampo` | campo ausente, chave fora do JSON nas quatro operações |
| `deveTratarObservacaoNullComoAusente` | `null` explícito, nada gravado |
| `deveTratarObservacaoEmBrancoComoAusente` | `""`, espaços e quebras de linha, parametrizado |
| `deveAceitarObservacaoCom500Caracteres` | limite exato aceito e recuperado |
| `deveRejeitarObservacaoCom501CaracteresSemCriarPedido` | 400 `VALIDATION_ERROR` com mensagem do campo e nenhum pedido gravado |
| `naoDeveAlterarObservacaoAoAdicionarItem` | `observacao` no corpo de `itens` é ignorada |

Em `PedidoTest` entraram dois testes de domínio, um para guardar o texto e tratar `null`, vazio
e espaços como ausência, e outro para o limite de 500 e 501 direto na entidade.

## 7. Resultados

### `mvn clean verify`

```
mvn clean verify  ->  BUILD SUCCESS
Testes unitários  31, falhas 0
Testes de API     20, falhas 0
All coverage checks have been met.
```

| Contador | Antes | Depois |
|---|---|---|
| Linhas | 700/715, 97,90% | 709/724, 97,93% |
| Branches | 107/132, 81,06% | 113/138, 81,88% |
| Instruções | 3738/3867, 96,66% | 3781/3910, 96,70% |

`Pedido` ficou com 100% das linhas. Nenhuma exclusão nova no JaCoCo e o pipeline não foi alterado.

### Validação da especificação OpenAPI

A especificação foi validada com duas ferramentas, nas versões `1.0.0` e `1.1.0`.

```
$ npx @redocly/cli lint docs/openapi/foodnow-pedidos.yaml
validating foodnow-pedidos.yaml...
foodnow-pedidos.yaml: validated in 30ms
Woohoo! Your API description is valid. 🎉

$ npx @apidevtools/swagger-cli validate docs/openapi/foodnow-pedidos.yaml
foodnow-pedidos.yaml is valid
```

O `docs/openapi/redocly.yaml` desliga só as regras de licença e de servidor em localhost, que não
se aplicam a um projeto didático que roda local. A regra de segurança foi atendida declarando
`security: []`, já que a API não tem autenticação.

Validar a sintaxe não garante que o documento bate com a aplicação. Por isso as respostas reais
foram conferidas contra os schemas da especificação com um script Ajv (JSON Schema 2020-12, o
dialeto do OpenAPI 3.1), com a aplicação rodando.

```
OK    201 criar com observacao
OK    201 criar sem observacao
OK    201 criar observacao null
OK    201 criar observacao 500
OK    400 criar observacao 501
OK    400 criar sem itens
OK    404 criar cliente inexistente
OK    422 criar endereco de outro
OK    200 consultar
OK    404 consultar inexistente
OK    200 adicionar item
OK    400 adicionar item qtd 0
OK    200 confirmar
OK    422 confirmar de novo
OK    exemplo PedidoCriado
OK    exemplo PedidoComObservacao
todas as respostas conferem com a especificacao
```

Para ter certeza de que o validador não aceitava qualquer coisa, também conferimos que ele
rejeita um objeto vazio, `observacao` vazia ou `null` na resposta e um request com 501
caracteres.

## 8. Fora do escopo

Estes pontos apareceram no levantamento e ficaram como estão, porque a atividade pede para
limitar a implementação ao requisito e preservar as regras atuais.

| Ponto | Por que ficou |
|---|---|
| itens aceitos em pedido já confirmado | é regra de itens, que a atividade pede para preservar |
| taxa de entrega não recalculada ao adicionar item | regra de valores, mesma razão |
| `observacao` numérica convertida em texto pelo Jackson, `123` vira `"123"` | coerção padrão do Jackson que vale para todos os campos string da API. Mudar isso é uma decisão global |
| JSON malformado sem `ErroResponse` | já era assim em todos os endpoints. Unificar mudaria o contrato de erro da API inteira |
| id não numérico devolvendo 422 | mesmo motivo, é comportamento de todos os endpoints com id |
| mensagens de validação em inglês | dependem do locale e já eram assim |
| perfil de teste sem o `non_null` global | o campo novo não depende dele. Igualar os perfis poderia mudar respostas de outros endpoints |
