# Atividade 01 — 2º bimestre: APIs como Fronteiras Arquiteturais

Integrante: Pedro Terra (`PedroTerra11`).

## Base e escopo

A implementação parte da `master` do professor, commit `c8eab129588411c8abe0b87cfc21a6ed4e1b3d68`, que já contém a refatoração de referência do primeiro bimestre. Esta entrega corresponde somente à Atividade 01 do segundo bimestre.

A observação é um dado do pedido, recebido uma única vez na criação. Não interfere nas regras de itens, cálculo de valores, pagamento ou entrega. Não foram acrescentados endpoints, dependências Maven ou exclusões de cobertura.

## Contrato anterior

O levantamento foi feito em `PedidoController`, `ApiDtos`, `PedidoService`, `Pedido`, `ApiExceptionHandler` e nos testes de API. A especificação [openapi-antes.json](openapi-antes.json) foi registrada em um commit anterior à implementação. A especificação [openapi.json](openapi.json) representa o contrato evoluído.

| Operação | Entrada | Sucesso | Erros previstos |
| --- | --- | --- | --- |
| `POST /pedidos` | JSON `PedidoRequest`: `clienteId`, `restauranteId`, `enderecoEntregaId` não nulos e lista `itens` não vazia | 201, `PedidoResponse`, estado `CRIADO` | 400 por validação; 404 para cliente, restaurante ou produto inexistente; 422 por endereço ou regra do produto/atendimento |
| `GET /pedidos/{id}` | `id` convertido para `Long`, sem corpo | 200, `PedidoResponse` | 400 para identificador não numérico; 404 para pedido inexistente |
| `POST /pedidos/{id}/itens` | `id` e JSON `ItemPedidoRequest`: `produtoId` não nulo, `quantidade >= 1` | 200, `PedidoResponse` atualizado | 400 por validação/conversão; 404 para pedido ou produto inexistente; 422 por produto indisponível, de outro restaurante ou sem atendimento ao endereço |
| `POST /pedidos/{id}/confirmar` | `id`, sem corpo | 200, `PedidoResponse`, estado `CONFIRMADO` | 400 para identificador não numérico; 404 para pedido inexistente; 422 para pedido sem itens ou fora do estado `CRIADO` |

Não foi acrescentada uma restrição de estado à inclusão de itens: a atividade preserva o comportamento que já existia.

`PedidoRequest.itens` utiliza validação em cascata de `ItemPedidoRequest`. Cada item exige produto e quantidade válida. `PedidoResponse` contém `id`, `clienteId`, `restauranteId`, `enderecoEntregaId`, `itens`, `subtotal`, `taxaEntrega`, `valorTotal`, `distanciaEntregaKm` e `status`. Cada `ItemResponse` contém `produtoId`, `nome`, `quantidade`, `precoUnitario` e `subtotal`.

`ApiExceptionHandler` retorna `ErroResponse(status, erro, mensagem)`: `VALIDATION_ERROR` em 400 de Bean Validation, `NOT_FOUND` em 404 e `REGRA_NEGOCIO` em 422. Erros de JSON ilegível e conversão do parâmetro são tratados pelo Spring e não têm a mesma garantia de corpo; essa diferença está registrada na especificação.

Os testes existentes relacionados ao contrato são:

- `FoodNowFlowIT`: criação, inclusão de item, confirmação, consulta, pagamento e entrega;
- `FoodNowErrorsIT`: produto indisponível, atendimento fora da área, lista vazia e pagamento rejeitado;
- `GeografiaContratoIT`: comparação integral das respostas e notificações em quatro cenários geográficos, além das restrições de endereço;
- `PedidoTest`: regras de itens, valores e transições do pedido.

## Decisão e comparação dos contratos

| Aspecto | Antes | Depois |
| --- | --- | --- |
| Criação | Sem campo específico para observação | `PedidoRequest.observacao`, opcional, com `@Size(max = 500)` |
| Ausência e `null` | Pedido criado normalmente | Mesmo comportamento; banco recebe `null` e resposta omite o campo |
| Texto vazio | Sem significado próprio no contrato | `""` é aceito e devolvido como `""` |
| Somente espaços | Sem significado próprio no contrato | Preservados, sem `trim` ou normalização |
| Texto informado | Não armazenado como dado do pedido | Armazenado e devolvido sem modificar o conteúdo |
| Limite | Não aplicável | 500 aceitos; 501 rejeitados com 400 e `VALIDATION_ERROR`, sem criar pedido |
| Respostas das quatro operações | `PedidoResponse` original | Mesmos campos e mais `observacao` quando não nula |
| Edição | Sem endpoint de observação | Sem endpoint e sem setter para edição |
| Persistência | Sem coluna | Coluna opcional de tamanho 500 na entidade `Pedido` |

O DTO valida o tamanho antes da entrada no serviço. O construtor da entidade também protege o limite para chamadas que não passam pela API. O limite utiliza a contagem de `String.length()`/Bean Validation do Java (unidades UTF-16); símbolos fora do plano básico, como alguns emojis, ocupam duas unidades. `maxLength: 500` na OpenAPI expressa o teto de comprimento, e esta regra de contagem específica do servidor também está documentada nos schemas.

`PedidoController` encaminha o campo a `PedidoService`, que cria a entidade com o texto. O mapeamento `PedidoResponse.from` é compartilhado pelas quatro operações, evitando respostas inconsistentes. A anotação `JsonInclude.NON_NULL` foi aplicada somente à observação da resposta; os demais campos não mudam. O H2 segue em memória e o esquema continua sendo criado pelo Hibernate, conforme a base.

## Consumidores e compatibilidade

Os consumidores efetivamente encontrados são os testes HTTP em `src/test/java/br/edu/foodnow/controller`. `GeografiaContratoIT` merece atenção porque compara o JSON inteiro com as referências em `src/test/resources/geografia`. Nenhum desses testes ou arquivos de referência foi alterado. Não foi encontrado um frontend ou aplicativo móvel neste repositório.

Aplicativos móveis, sites, SDKs gerados e integrações de restaurantes são consumidores hipotéticos. Um cliente que rejeita propriedades desconhecidas pode precisar de atualização ao receber uma observação. Esses clientes não foram identificados na base e não são apresentados como integrações existentes.

A mudança é compatível para clientes que continuam enviando os campos anteriores: as entradas antigas continuam válidas e suas respostas mantêm a estrutura anterior porque o campo nulo é omitido. Para pedidos com observação, a compatibilidade pressupõe consumidores capazes de aceitar um campo adicional. Um consumidor estrito que consulte um pedido criado por outro cliente com observação pode precisar de ajuste. Também deve ser avaliado o caso de um cliente que já enviava uma propriedade desconhecida chamada `observacao`: ela passa a ter significado e validação.

Não foi criada `/v2`, pois não há remoção, renomeação, mudança de tipo ou nova obrigatoriedade nos campos anteriores. A especificação passou de 1.0.0 para 1.1.0 para registrar a evolução documental; isso não cria uma versão de rota nem altera a versão Maven. Se surgirem consumidores estritos que não possam ser atualizados, a estratégia de compatibilidade deverá ser reavaliada.

## Validação

`PedidoObservacaoIT` cobre o texto informado, a consulta posterior, a manutenção ao adicionar itens e confirmar, ausência, `null`, vazio, espaços, texto com espaços nas extremidades, 500 caracteres e rejeição de 501, inclusive quando todos são espaços. A rejeição verifica também que a quantidade de pedidos persistidos não aumenta. Um caso adicional envia uma tentativa de substituição junto ao item e confirma que a observação original permanece. `PedidoObservacaoTest` verifica a proteção do limite diretamente na entidade.

Os resultados e comandos estão em [validacao.md](validacao.md).
