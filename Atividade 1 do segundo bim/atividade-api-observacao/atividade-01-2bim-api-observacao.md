# Atividade 01 - 2BIM: APIs como Fronteiras Arquiteturais

**Integrante(s):** Pedro [+ nome da dupla, se aplicável]

> **Nota de escopo.** Fiz este documento sem acesso ao repositório real do FoodNow.
> O contrato "atual" (seção 1 e `openapi-antes.yaml`) foi **reconstruído a partir do enunciado**,
> com nomes de campos, códigos de erro e exemplos plausíveis. Antes de entregar, confira cada
> item contra os DTOs, controllers e testes reais e ajuste o que divergir. O código da seção 4
> é ilustrativo (Spring Boot + Bean Validation). Os resultados de `mvn clean verify` e a
> cobertura **não foram executados** e estão como roteiro na seção 7.

## 1. Análise do contrato atual

| Operação | Parâmetros | Corpo | Sucesso | Erros previstos |
|---|---|---|---|---|
| `POST /pedidos` | nenhum | `CriarPedidoRequest` (clienteId, restauranteId, latitude, longitude) | 201 + `PedidoResponse` | 400 (validação), 404 (cliente/restaurante), 422 (fora da área) |
| `GET /pedidos/{id}` | path `id` | nenhum | 200 + `PedidoResponse` | 404 |
| `POST /pedidos/{id}/itens` | path `id` | `ItemPedidoRequest` (produtoId, quantidade ≥ 1) | 200 + `PedidoResponse` | 400, 404, 409 (estado inválido) |
| `POST /pedidos/{id}/confirmar` | path `id` | nenhum | 200 + `PedidoResponse` | 404, 409 |

**DTOs:** `CriarPedidoRequest`, `ItemPedidoRequest` (entrada); `PedidoResponse`, `ItemPedidoResponse`, `ErroResponse` (saída).
As quatro operações devolvem `PedidoResponse`, o que importa: **um único campo novo em `PedidoResponse` aparece nas quatro respostas**.

**Testes existentes (a mapear no repositório):** um teste de API por operação e cenário de status
(201/400/404/422, 200/404, 200/400/404/409, 200/404/409). Eles continuam valendo sem alteração.

## 2. Especificação OpenAPI do contrato atual

Arquivo: `openapi-antes.yaml` (OpenAPI 3.0.3). Contém schemas, parâmetro de path, request bodies,
respostas de sucesso e de erro e exemplos. Validada com `openapi-spec-validator`.

## 3. Contrato da evolução

**Campo:** `observacao` (string, opcional, `maxLength: 500`).
- **DTOs afetados:** `CriarPedidoRequest` (entrada, novo campo) e `PedidoResponse` (saída, novo campo).
  `ItemPedidoRequest` e `ItemPedidoResponse` **não mudam**.
- **Operações afetadas:** `POST /pedidos` (aceita o campo) e as quatro respostas (devolvem o campo).
  Não há edição posterior: nenhuma operação recebe `observacao` além de `POST /pedidos`.

**Tratamento dos casos:**

| Entrada | Comportamento | Resposta |
|---|---|---|
| Campo ausente | pedido criado normalmente | `observacao: null` |
| `"observacao": null` | igual a ausente | `observacao: null` |
| `""` ou só espaços | tratado como "sem observação" (normalizado para `null`) | `observacao: null` |
| Texto com 1 a 500 caracteres | armazenado como enviado (com `trim()` nas bordas) | devolvido igual |
| 501 ou mais caracteres | rejeitado, nada é persistido | 400 + `ErroResponse` |

Decisão: texto vazio vira `null` para existir um único estado "sem observação" e o consumidor
não precisar tratar `""` e `null` de forma diferente. A contagem dos 500 caracteres é feita
**depois** do `trim()`, e a regra está documentada no schema.

**Consumidores:**

| Consumidor | Origem | Impacto |
|---|---|---|
| Aplicativo do Cliente (Flutter) | hipotético (arquitetura do FoodNow; não está no repositório da atividade) | ignora o campo novo se o parser for tolerante; passa a poder enviá-lo |
| Portal do Restaurante (React) | hipotético | idem; é o maior beneficiário (precisa **exibir** a observação) |
| Aplicativo do Entregador (Flutter) | hipotético | idem |
| Testes de API existentes | **encontrados no repositório** | continuam passando se não usam comparação estrita do JSON inteiro |
| Integrações/mocks de pagamento e entrega | **encontrados no repositório** (simulados) | não leem `observacao`; sem impacto |

**Classificação: mudança compatível (aditiva), sob estas condições:**
1. o campo é opcional na entrada, então clientes antigos que não o enviam continuam válidos;
2. consumidores de resposta ignoram propriedades desconhecidas (leitura tolerante);
3. nenhum campo existente muda de nome, tipo, obrigatoriedade ou semântica;
4. nenhum teste ou cliente compara o JSON de resposta por igualdade estrita (se houver, é
   **o teste** que precisa de ajuste, e isso deve ser justificado no PR).

Se a condição 2 ou 4 falhar para algum consumidor real, aquele consumidor é afetado
pontualmente, mas o contrato continua evolutivamente compatível para o conjunto.

**Versionamento:** **não é necessária uma nova versão** (`/v2`). Não há quebra, e o enunciado diz
que `/v2` não é requisito. Registra-se apenas a bump de `info.version` de `1.0.0` para `1.1.0`
(minor, aditivo) na especificação.

## 4. Implementação (ilustrativa)

```java
// CriarPedidoRequest.java
public record CriarPedidoRequest(
        @NotNull Long clienteId,
        @NotNull Long restauranteId,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @Size(max = 500, message = "observacao deve ter no maximo 500 caracteres")
        String observacao   // opcional: sem @NotNull
) {}

// Pedido.java (entidade)
@Column(name = "observacao", length = 500)
private String observacao;

// PedidoService.criarPedido(...)
String obs = req.observacao() == null ? null : req.observacao().trim();
pedido.setObservacao(obs == null || obs.isEmpty() ? null : obs);

// PedidoResponse.java (acrescentar o campo; nenhum outro muda)
public record PedidoResponse(Long id, String status, Long clienteId, Long restauranteId,
        List<ItemPedidoResponse> itens, BigDecimal subtotal, BigDecimal taxaEntrega,
        BigDecimal total, String observacao) {}
```

```sql
-- migração (Flyway/Liquibase): coluna nullable, não exige backfill
ALTER TABLE pedido ADD COLUMN observacao VARCHAR(500) NULL;
```

Pontos de atenção:
- O `@Size(max = 500)` sozinho conta antes do `trim()`. Para garantir "500 após trim", valide
  também no service (ou use um validador próprio) e lance a mesma exceção que o handler já
  converte em 400.
- Se a serialização Jackson do projeto usa `NON_NULL`, `observacao: null` **não aparece** no
  JSON. Confira a configuração: o contrato documentado deve bater com o JSON efetivo
  (`nullable: true` na spec pressupõe que o campo é emitido como `null`).
- Pedidos já existentes têm `observacao = NULL`, o que dá o mesmo resultado que "ausente".
- Regras de itens, valores, pagamento e entrega não são tocadas.

## 5. Comparação antes/depois

O diff completo está em `comparacao-contrato.diff`. Resumo:

| Elemento | Antes (1.0.0) | Depois (1.1.0) |
|---|---|---|
| `CriarPedidoRequest.observacao` | não existe | opcional, string, `maxLength: 500`, nullable |
| `PedidoResponse.observacao` | não existe | string nullable, `maxLength: 500` |
| `POST /pedidos` 400 | só violações de campos obrigatórios | também `observacao` com mais de 500 |
| `/itens` e `/confirmar` | sem o campo na resposta | devolvem `observacao` mantida |
| Campos, status e rotas existentes | - | inalterados |
| `info.version` | 1.0.0 | 1.1.0 |

## 6. Testes de API a adicionar (e a preservar)

**Preservar:** todos os testes existentes, sem edição.

**Adicionar** (MockMvc/RestAssured, mesmo estilo dos testes atuais):

| # | Cenário | Esperado |
|---|---|---|
| 1 | POST com `observacao: "Não incluir talheres"` | 201, resposta com a mesma observação |
| 2 | GET do pedido criado em (1) | 200, observação mantida |
| 3 | POST `/itens` no pedido de (1) | 200, observação mantida |
| 4 | POST `/confirmar` no pedido de (1) | 200, observação mantida |
| 5 | POST sem o campo | 201, `observacao` nulo |
| 6 | POST com `observacao: null` | 201, `observacao` nulo |
| 7 | POST com `""` e com `"   "` | 201, `observacao` nulo |
| 8 | POST com exatamente 500 caracteres | 201, 500 caracteres devolvidos |
| 9 | POST com 501 caracteres | 400, pedido **não** persistido (GET/contagem confirma) |
| 10 | Pedido sem observação segue em `/itens` e `/confirmar` | 200, comportamento anterior |
| 11 | Regressão: valores de subtotal, taxa e total | idênticos aos do contrato anterior |

```java
@Test
void aceitaExatamente500CaracteresERejeita501() throws Exception {
    String ok = "a".repeat(500), longa = "a".repeat(501);
    mvc.perform(post("/pedidos").contentType(JSON).content(corpoComObservacao(ok)))
       .andExpect(status().isCreated())
       .andExpect(jsonPath("$.observacao").value(ok));
    mvc.perform(post("/pedidos").contentType(JSON).content(corpoComObservacao(longa)))
       .andExpect(status().isBadRequest());
}
```

## 7. Roteiro de verificação (não executado aqui)

1. `mvn clean verify` **antes** das mudanças: guarde testes e cobertura (JaCoCo).
2. Aplicar a implementação e os testes.
3. `mvn clean verify` de novo. Conferir: testes antigos intactos, novos passando, cobertura de
   linhas ≥ 80% **sem novas exclusões** no JaCoCo, pipeline verde.
4. Validar a spec: `npx @redocly/cli lint openapi-depois.yaml` ou `openapi-spec-validator`
   (já passei as duas specs deste pacote pelo `openapi-spec-validator`).
5. Conferir, num teste, que o JSON real de `PedidoResponse` bate com o schema (principalmente o
   `null` explícito de `observacao`).

## Texto sugerido para o Pull Request

```markdown
## Atividade 01 - 2BIM: APIs como Fronteiras Arquiteturais

**Integrante(s):** Pedro [+ dupla]

### Análise do contrato e compatibilidade
Contrato das 4 operações de pedidos documentado em OpenAPI (antes: openapi-antes.yaml).
Nova `observacao` opcional (até 500) em `CriarPedidoRequest` e `PedidoResponse`.
Mudança **compatível/aditiva** nas condições: campo opcional na entrada, consumidores com
leitura tolerante, nenhum campo existente alterado. Sem nova versão da API (sem /v2);
`info.version` 1.0.0 -> 1.1.0.

### Tratamento definido
Ausente/null/vazio/só espaços -> `null`; 1-500 -> armazenado; 501+ -> 400.

### Implementação e testes
Coluna `observacao VARCHAR(500) NULL`; validação Bean Validation + normalização no service;
11 novos testes de API (informada, recuperação em GET/itens/confirmar, ausência, null, vazio,
500 e 501); testes existentes preservados.

### Comparação antes/depois
Ver `comparacao-contrato.diff` e tabela da documentação.

### mvn clean verify / cobertura / validação OpenAPI
[preencher: resultado do build, % de cobertura, saída do validador]
```
