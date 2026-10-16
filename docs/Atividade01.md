# Análise Contrato

| Operação | Entrada e validação | Resposta e status |
|---|---|---|
| `POST /pedidos` | JSON `PedidoRequest`: `clienteId`, `restauranteId` e `enderecoEntregaId` não nulos; `itens` não vazio. Cada `ItemPedidoRequest` exige `produtoId` não nulo e `quantidade >= 1`. | `PedidoResponse`, `201`; `400` para validação, `404` para cliente/restaurante inexistentes e `422` para regras de negócio (por exemplo, endereço fora da área, produto indisponível ou inválido). |
| `GET /pedidos/{id}` | `id` numérico no caminho. Sem corpo. | `PedidoResponse`, `200`; `400` se o `id` não puder ser convertido para número; `404` se o pedido não existir. |
| `POST /pedidos/{id}/itens` | `id` no caminho e JSON `ItemPedidoRequest`; mesmas validações de produto e quantidade. | `PedidoResponse`, `200`; `400` para validação, `404` para pedido/produto inexistentes e `422` para regras de negócio. |
| `POST /pedidos/{id}/confirmar` | `id` numérico no caminho. Sem corpo. | `PedidoResponse`, `200`; `400` se o `id` não puder ser convertido para número; `404` se o pedido não existir e `422` se não puder ser confirmado (por exemplo, pedido vazio ou não criado). |


Os DTOs e regras estão em `ApiDtos.PedidoRequest`, `ApiDtos.ItemPedidoRequest`, `ApiDtos.PedidoResponse` e `ApiDtos.ItemResponse`; as rotas estão em `PedidoController`, a regra de criação em `PedidoService` e a persistência na entidade JPA `Pedido`. Os testes de API existentes são `FoodNowFlowIT` (fluxo de criação, inclusão de item, confirmação, pagamento e consulta) e `FoodNowErrorsIT` (erros/regras e validação de pedido sem itens); `ApiTestSupport` cria pedidos sem observação.

## Contrato proposto e implementado

- `POST /pedidos` aceita `observacao`, tipo string opcional e anulável, com no máximo 500 caracteres. Não é permitido alterá-la depois da criação.
- Campo ausente ou `null`, string vazia e string composta somente por espaços são tratados como ausência. Nesse caso não há valor persistido e a propriedade é omitida no JSON por política de inclusão explícita do DTO (além da configuração global).
- Uma observação não vazia é armazenada sem alteração, limitada também a 500 caracteres pela coluna da entidade e pela validação do DTO.
- `PedidoResponse` inclui `observacao` quando há valor. `GET /pedidos/{id}`, `POST /pedidos/{id}/itens` e `POST /pedidos/{id}/confirmar` mantêm e retornam o mesmo valor.
- Texto com mais de 500 caracteres recebe `400` (`VALIDATION_ERROR`). Os demais status, campos, regras de itens, valores, pagamento e entrega permanecem iguais.
- O campo não é aceito em `ItemPedidoRequest` nem é disponibilizado como campo mutável ou endpoint de edição.

A especificação OpenAPI correspondente está em [`openapi.yaml`](./openapi.yaml).

## Comparação e compatibilidade

| Superfície | Anterior | Proposto |
|---|---|---|
| Corpo de criação | Sem observação; os quatro dados de negócio já descritos eram obrigatórios conforme as validações. | Campo opcional `observacao` (`string`/`null`, máximo 500); nenhum campo anterior muda de obrigatoriedade. |
| Persistência | Pedido sem campo de observação. | Coluna anulável `observacao` com limite 500; sem operação de edição. |
| Respostas do pedido | DTO com os dez campos listados acima. | Acrescenta propriedade opcional `observacao`, omitida sem valor. |
| Status e regras | Códigos e validações descritos acima. | Mantidos; erro de limite excedido usa o `400` de validação já existente. |

## Testes de API adicionados

O fluxo cobre a observação informada na criação, consulta, adição de item, confirmação e consulta após pagamento. Os testes de erros exercitam ausência, `null`, vazio, somente espaços, limite de 500 e rejeição de 501 caracteres. Os testes anteriores de criação sem o campo permanecem ativos.
