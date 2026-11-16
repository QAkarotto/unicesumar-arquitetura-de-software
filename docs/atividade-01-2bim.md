# Atividade 01 — APIs como fronteiras arquiteturais

## Identificação

- Integrantes: Danilo Bolzan de Moraes e Edi Carlos Francisco Junior
- Base analisada: `master`, após a solução de referência da Atividade 06.

## Contrato anterior

| Operação | Entrada | Validações | Sucesso | Erros observáveis | DTOs e testes existentes |
|---|---|---|---|---|---|
| `POST /pedidos` | JSON `PedidoRequest`: `clienteId`, `restauranteId`, `enderecoEntregaId` obrigatórios; `itens` não vazio; cada item contém `produtoId` obrigatório e `quantidade >= 1` | Bean Validation, existência dos recursos, vínculo do endereço, disponibilidade/entrega/restaurante do produto | `201` + `PedidoResponse` | `400` validação, `404` recurso inexistente, `422` regra de negócio | `PedidoRequest`, `ItemPedidoRequest`, `PedidoResponse`; `FoodNowFlowIT`, `FoodNowErrorsIT`, `GeografiaContratoIT`, `ApiTestSupport` |
| `GET /pedidos/{id}` | Parâmetro de caminho `id` (`Long`) | Pedido existente | `200` + `PedidoResponse` | `404` pedido inexistente | `PedidoResponse`; `FoodNowFlowIT`, `FoodNowErrorsIT`, `GeografiaContratoIT` |
| `POST /pedidos/{id}/itens` | `id` no caminho; JSON `ItemPedidoRequest` | Pedido/produto existentes, quantidade, disponibilidade, entrega e restaurante | `200` + `PedidoResponse` | `400`, `404`, `422` | `ItemPedidoRequest`, `PedidoResponse`; `FoodNowFlowIT`, `GeografiaContratoIT` |
| `POST /pedidos/{id}/confirmar` | `id` no caminho; sem corpo | Pedido existente, com itens e no estado `CRIADO` | `200` + `PedidoResponse` | `404`, `422` | `PedidoResponse`; `FoodNowFlowIT`, `FoodNowErrorsIT`, `GeografiaContratoIT` |

O `PedidoResponse` anterior contém `id`, identificadores relacionados, itens, subtotal, taxa e valor total, distância e status. A representação completa do contrato anterior está em `docs/openapi-pedidos-anterior.yaml`.

## Evolução proposta

Foi acrescentado `observacao` somente ao `PedidoRequest` e ao `PedidoResponse`:

- propriedade JSON opcional, do tipo `string` ou `null`, com no máximo 500 caracteres;
- campo ausente ou `null`: aceito e tratado como ausência;
- texto vazio ou composto somente por espaços: tratado como ausência;
- quando ausente, o valor não é persistido e a propriedade é omitida do JSON;
- texto de 500 caracteres: aceito e preservado;
- texto de 501 caracteres: rejeitado com `400` e `VALIDATION_ERROR`;
- observações não vazias são preservadas sem remoção de espaços e não podem ser editadas posteriormente;
- todas as operações que retornam `PedidoResponse` expõem o mesmo valor persistido.

A persistência usa coluna de comprimento 500 e a entrada usa `@Size(max = 500)`. A especificação atualizada está em `docs/openapi-pedidos.yaml`.

## Consumidores e compatibilidade

Consumidores encontrados no repositório:

- testes de API `FoodNowFlowIT`, `FoodNowErrorsIT` e `GeografiaContratoIT`;
- utilitário de testes `ApiTestSupport`;
- `PagamentoService`, `EntregaService` e `NotificacaoService` consomem o modelo internamente, mas não constroem o DTO HTTP de entrada;
- não foi encontrado cliente externo, frontend ou SDK no repositório.

Consumidores hipotéticos: aplicativos web/mobile, integrações de restaurantes, scripts e SDKs que criem ou leiam pedidos.

A mudança é compatível sob as condições usuais de evolução aditiva de JSON: o servidor continua aceitando requisições antigas sem o campo e consumidores toleram propriedades desconhecidas nas respostas. Um consumidor com desserialização estrita, schema fechado (`additionalProperties: false`) ou comparação textual exata da resposta pode quebrar; nesse caso, a mudança seria incompatível para esse consumidor específico. Como o requisito não altera campos existentes, semântica, rota ou status e mantém as requisições antigas, não foi criada `/v2`.

## Comparação antes/depois

| Aspecto | Antes | Depois |
|---|---|---|
| Criação | Sem observação | Aceita `observacao` opcional, anulável e com `maxLength: 500` |
| Persistência | Pedido não armazenava o dado | Pedido armazena o valor sem edição posterior |
| Respostas | Sem `observacao` | Incluem `observacao` somente quando há valor |
| Rotas, status e regras | Contrato vigente | Preservados |
| Versionamento | API sem versão nova | Mantido; evolução aditiva condicionada à tolerância a novos campos |

## Validação

Os testes adicionados cobrem observação informada, recuperação por `GET`, manutenção ao adicionar item e confirmar, ausência, `null`, vazio, somente espaços e os limites de 500/501 caracteres.

- `mvn --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS**;
- testes unitários: **29 aprovados**;
- testes de API/integração: **14 aprovados**;
- cobertura de linhas JaCoCo: **707 / 723 (97,79%)**, acima do mínimo de 80%;
- `npx --yes @redocly/cli lint docs/openapi-pedidos-anterior.yaml docs/openapi-pedidos.yaml`: descrições **válidas**, sem erros (quatro avisos não bloqueantes sobre licença acadêmica sem URL e servidor local).

As referências JSON de `GeografiaContratoIT` permanecem inalteradas: pedidos antigos sem observação continuam produzindo exatamente o contrato anterior, sem a propriedade opcional.

O teste de 501 caracteres verifica o campo inválido e o limite informado sem depender do idioma configurado para as mensagens do Bean Validation, mantendo o mesmo resultado em ambiente local e no CI.
