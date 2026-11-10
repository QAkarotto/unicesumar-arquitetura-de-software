# Atividade 01 - 2BIM: APIs como Fronteiras Arquiteturais - Solução

## Integrantes

| Nome | RA |
|---|---|
| Lucas Wessendorf de Araujo | 23351656-2 |
| Eduardo Alan dos Santos | 23074991-2 |

## Base utilizada

Base refatorada disponibilizada pelo professor (`origin/master`, commit `29c4341 refactor: concentra politica logistica e prepara base da atividade 06`).

## 1. Análise do contrato atual (v1.0.0)

Fontes analisadas: `PedidoController`, `ApiDtos`, `ApiExceptionHandler`, `PedidoService`, `Pedido`, e as respostas reais obtidas executando a aplicação.

| Operação | Parâmetros | Corpo | Sucesso | Erros |
|---|---|---|---|---|
| `POST /pedidos` | — | `PedidoRequest` | `201` + `PedidoResponse` | `400` validação/corpo ilegível, `404` cliente/restaurante/produto, `415` mídia, `422` regra de negócio |
| `GET /pedidos/{id}` | `id` (path, int64) | — | `200` + `PedidoResponse` | `404` pedido, `422` `id` não numérico |
| `POST /pedidos/{id}/itens` | `id` (path, int64) | `ItemPedidoRequest` | `200` + `PedidoResponse` | `400`, `404` pedido/produto, `415`, `422` |
| `POST /pedidos/{id}/confirmar` | `id` (path, int64) | — | `200` + `PedidoResponse` | `404` pedido, `422` sem itens ou status ≠ `CRIADO` |

### DTOs e validações

| DTO | Campos | Validações |
|---|---|---|
| `PedidoRequest` | `clienteId`, `restauranteId`, `enderecoEntregaId`, `itens` | `@NotNull` nos três IDs, `@NotEmpty` em `itens`, `@Valid` em cada item |
| `ItemPedidoRequest` | `produtoId`, `quantidade` | `@NotNull produtoId`, `@Min(1) quantidade` (`int`: ausente gera `400` do framework) |
| `PedidoResponse` | `id`, `clienteId`, `restauranteId`, `enderecoEntregaId`, `itens`, `subtotal`, `taxaEntrega`, `valorTotal`, `distanciaEntregaKm`, `status` | — |
| `ItemResponse` | `produtoId`, `nome`, `quantidade`, `precoUnitario`, `subtotal` | — |
| `ErroResponse` | `status`, `erro` (`VALIDATION_ERROR`, `NOT_FOUND`, `REGRA_NEGOCIO`), `mensagem` | — |

Comportamentos do contrato atual, confirmados com a aplicação em execução e preservados:

- Campos desconhecidos no corpo são **ignorados**. Antes da evolução, `"observacao"` enviada era descartada sem erro.
- JSON malformado, `quantidade` ausente e `Content-Type` diferente de JSON retornam o erro padrão do Spring (`timestamp`, `status`, `error`, `path`), e não `ErroResponse`.
- `id` não numérico no path retorna `422 REGRA_NEGOCIO`, porque a exceção de conversão cai no handler de `IllegalArgumentException`.
- `POST /pedidos/{id}/itens` não restringe pelo status do pedido.
- Existe uma divergência de serialização entre ambientes: `src/main/resources/application.properties` define `spring.jackson.default-property-inclusion=non_null`, mas o `application.properties` de teste substitui esse arquivo. Nos testes, campos `null` são serializados; em produção, são omitidos.

### Testes existentes relacionados

| Teste | O que cobre do contrato de pedidos |
|---|---|
| `FoodNowFlowIT` | Fluxo feliz das quatro operações (`201`/`200`, `status`, `subtotal`, `valorTotal`) |
| `FoodNowErrorsIT` | `422` produto indisponível e fora da área, `400` pedido sem itens |
| `GeografiaContratoIT` | Snapshot do JSON completo de criação, inclusão de item, confirmação e consulta (compara o mapa inteiro com as referências em `src/test/resources/geografia`) |
| `PedidoTest` | Regras de domínio: cálculo de valores, quantidade, restaurante, confirmação |

### Especificação do contrato atual

`docs/openapi/foodnow-pedidos.yaml`, versão **1.0.0**, criada **antes** da implementação no commit `docs: especifica em OpenAPI o contrato atual das operações de pedidos`. Ela contém schemas, parâmetros, request bodies, respostas de sucesso e de erro e exemplos.

## 2. Contrato proposto (v1.1.0)

### Campo e DTOs afetados

| DTO | Mudança |
|---|---|
| `PedidoRequest` | novo campo opcional `observacao: string` (`@Size(max = 500)`) |
| `PedidoResponse` | novo campo opcional `observacao: string`, com `@JsonInclude(NON_NULL)` |
| `ItemPedidoRequest`, `ItemResponse`, `ErroResponse` | sem alteração |

Persistência: coluna `observacao VARCHAR(500)` anulável em `Pedido`, preenchida só no construtor e sem setter. Isso atende ao requisito de não haver edição posterior.

### Tratamentos definidos

| Entrada em `POST /pedidos` | Resultado | Resposta |
|---|---|---|
| campo ausente | pedido sem observação | campo `observacao` **omitido** |
| `null` | igual a ausente | campo omitido |
| `""` ou só espaços | igual a ausente | campo omitido |
| `"  Sem cebola  "` | armazenado sem espaços nas bordas | `"observacao": "Sem cebola"` |
| até 500 caracteres | armazenado | campo presente |
| mais de 500 caracteres (valor recebido) | `400 VALIDATION_ERROR` | `mensagem: "observacao: tamanho deve ser entre 0 e 500"` |

Justificativas:

- **Vazio = ausente, em vez de rejeitar.** Formulários costumam enviar `""` quando o usuário não digita nada. Rejeitar esse valor criaria erro para um cliente sem nenhum benefício, e armazenar `""` criaria dois jeitos de representar "sem observação".
- **Omitir o campo, em vez de devolver `null`.** É a política global de produção (`non_null`). Com a anotação explícita no campo, o comportamento fica igual em produção e nos testes. Com isso, o JSON de pedidos sem observação continua **idêntico ao da v1.0.0**, o que os snapshots de `GeografiaContratoIT` comprovam sem nenhuma alteração.
- **Limite sobre o valor recebido.** A validação ocorre na fronteira (Bean Validation, `400` no formato `VALIDATION_ERROR` já usado pela API). O domínio também garante o limite (`IllegalArgumentException`), como defesa caso `Pedido` seja criado por outro caminho.
- **Unidade do limite.** `@Size` conta unidades UTF-16 (`String.length()`), então um emoji conta como 2. A especificação documenta essa regra, para manter o contrato consistente com a validação real.
- **Sem edição.** Em `POST /pedidos/{id}/itens`, um campo `observacao` no corpo é ignorado, como qualquer campo desconhecido. Coberto por teste.

## 3. Consumidores potencialmente afetados

**Encontrados no repositório:**

| Consumidor | Impacto |
|---|---|
| `FoodNowFlowIT`, `FoodNowErrorsIT` (via `ApiTestSupport.criarPedido`) | Nenhum: não enviam o campo e não verificam campos ausentes |
| `GeografiaContratoIT` (snapshots de JSON completo) | Seria o consumidor mais sensível: comparação exata do mapa JSON. Sem impacto porque o campo é omitido quando ausente |
| `PagamentoService`, `EntregaService`, `NotificacaoService`, `LogisticaService` | Consomem a entidade `Pedido`, não o JSON. O construtor de 3 argumentos foi mantido, e nenhuma regra de valores, pagamento ou entrega mudou |
| `PoliticaEntregaAtualTest`, `LocalizacaoEntregaTest`, `PedidoTest` | Usam `new Pedido(cliente, restaurante, endereco)`, que continua existindo |

**Hipotéticos (não existem no repositório):**

| Consumidor | Risco |
|---|---|
| App mobile / front-end web do cliente | Leitor tolerante ignora o campo novo. Para usar a funcionalidade, precisa enviar `observacao` |
| Painel do restaurante | Precisaria exibir a observação (é o principal interessado); não quebra se ignorar |
| Clientes gerados a partir de OpenAPI ou com desserialização estrita (`FAIL_ON_UNKNOWN_PROPERTIES`, `additionalProperties: false`) | **Podem quebrar** ao receber o campo novo em pedidos com observação |
| Validação de schema em gateway, testes de contrato (Pact) e ETL/BI com schema fixo | Podem rejeitar o campo novo; o schema precisa ser atualizado |
| Cliente legado que já enviava `observacao` (antes ignorada) | Passa a ter o valor validado e armazenado. Um valor com mais de 500 caracteres, antes aceito, passa a gerar `400` |
| Cliente novo apontando para um servidor 1.0.0 | O campo é ignorado sem erro. O cliente deve conferir se `observacao` voltou na resposta |

## 4. Compatibilidade e versionamento

**Classificação: mudança compatível com versões anteriores (aditiva).**

Condições dessa avaliação:

1. O campo de entrada é **opcional**: toda requisição válida na 1.0.0 continua válida e produz o mesmo resultado.
2. Nenhum campo foi removido, renomeado ou teve tipo, obrigatoriedade ou semântica alterados, e nenhum status HTTP mudou.
3. O campo de saída só aparece em pedidos criados com observação, o que só um consumidor ciente da 1.1.0 faz. Pedidos sem observação produzem JSON idêntico ao anterior.
4. Os consumidores seguem o princípio do leitor tolerante (ignoram campos desconhecidos), que é o comportamento padrão do próprio FoodNow e do Jackson no Spring Boot.

A mudança **seria incompatível** para consumidores que violam a condição 4 (desserialização estrita) ou para o caso marginal de clientes que já enviavam `observacao` com mais de 500 caracteres. Nenhum desses casos existe no repositório.

**Decisão: não criar nova versão da API (sem `/v2`).** Uma nova versão da API se justifica quando há quebra de contrato para consumidores existentes, o que não ocorre nas condições acima. Criar `/v2` duplicaria endpoints e manutenção sem necessidade demonstrada, contrariando a restrição da atividade. A evolução é registrada como **versão minor da especificação (1.0.0 → 1.1.0)**, seguindo versionamento semântico, com histórico em `info.description`.

## 5. Comparação antes/depois

| Aspecto | 1.0.0 | 1.1.0 |
|---|---|---|
| `PedidoRequest` | 4 campos obrigatórios | + `observacao` opcional (`string`, `nullable`, `maxLength: 500`) |
| `PedidoResponse` | 10 campos obrigatórios | + `observacao` opcional (`string`, `minLength: 1`, `maxLength: 500`), omitida quando ausente |
| Novos erros | — | `400 VALIDATION_ERROR` para observação com mais de 500 caracteres (já coberto pela resposta `RequisicaoInvalida`) |
| Endpoints, métodos, status, outros schemas | — | inalterados |
| Exemplos | pedido simples | + request e response com observação, + erro de limite |

O diff completo da especificação está nos commits (`git diff 99f7f6f f54a57f -- docs/openapi/foodnow-pedidos.yaml`).

## 6. Testes

| Requisito | Teste |
|---|---|
| Observação informada é retornada na criação | `ObservacaoPedidoIT.deveArmazenarERetornarObservacaoEmTodasAsOperacoesDePedido` |
| Recuperação (`GET`) e manutenção em `/itens`, `/confirmar` e após pagamento | idem |
| Não há edição posterior | `naoDevePermitirEditarObservacaoAoAdicionarItem` |
| Ausência do campo (pedido antigo continua funcionando, campo omitido nas 4 operações) | `pedidoSemObservacaoDeveContinuarFuncionandoSemOCampoNaResposta` |
| `null` | `observacaoNulaDeveSerTratadaComoAusente` |
| Texto vazio / só espaços | `observacaoVaziaOuEmBrancoDeveSerTratadaComoAusente` (parametrizado) |
| Remoção de espaços nas bordas | `deveRemoverEspacosDasBordasDaObservacao` |
| Limite de 500 (aceito) | `deveAceitarObservacaoCom500Caracteres` |
| Limite de 501 (rejeitado) | `deveRejeitarObservacaoCom501Caracteres` |
| Limite aplicado ao valor recebido | `limiteDeveSerAplicadoAoValorRecebidoAntesDeRemoverEspacos` |
| Regras no domínio | `PedidoTest` (4 testes novos) |
| JSON real conforme a especificação (com e sem observação, erros) | `ContratoOpenApiIT` (3 testes) |

Testes existentes: **nenhum foi alterado ou removido**, já que a mudança é compatível. Os snapshots de `GeografiaContratoIT` continuam passando sem modificação.

## 7. Evidências

### `mvn clean verify` (JDK 26, Maven 3.9.14)

```
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0      (unitários - Surefire)
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0      (integração - Failsafe)
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

Antes da mudança: 29 unitários e 11 de integração. Nenhuma exclusão nova no JaCoCo.

**Cobertura de linhas (JaCoCo): 98,21%** (712 de 725 linhas; mínimo exigido: 80%). `Pedido`, `PedidoController`, `ApiDtos.PedidoRequest` e `ApiDtos.PedidoResponse` estão com 100% das linhas cobertas.

### Validação da especificação OpenAPI

1. **Redocly CLI** (`npx @redocly/cli lint docs/openapi/foodnow-pedidos.yaml`):
   ```
   Woohoo! Your API description is valid. 🎉
   You have 2 warnings.
   ```
   Os 2 avisos restantes são intencionais: `info-license` (o repositório não define licença) e `no-server-example.com` (o servidor real do projeto é `localhost:8080`).
2. **swagger-cli** (`npx @apidevtools/swagger-cli validate docs/openapi/foodnow-pedidos.yaml`):
   ```
   docs/openapi/foodnow-pedidos.yaml is valid
   ```
3. **Validação automatizada no pipeline:** `ContratoOpenApiIT` carrega a especificação com o `swagger-parser` (falha se houver mensagens de erro) e usa o `openapi-request-validator` para validar requisições e respostas **reais** das quatro operações contra o YAML. Também falha se a API devolver algum campo não documentado. Como roda no `mvn clean verify`, o CI garante que contrato documentado e JSON retornado não divirjam. Em um teste de sanidade, alterar o tipo de `valorTotal` no YAML fez o teste falhar.
