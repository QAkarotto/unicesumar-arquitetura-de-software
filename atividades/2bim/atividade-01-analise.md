# Atividade 01 – APIs como Fronteiras Arquiteturais

## Identificação

- Integrante: Ramon Albini Vieira e Eduardo Henrique
- Projeto: FoodNow
- Branch: feature/eng-soft8s/ramon-albini/atividade1-2Bim

## 1. Análise do contrato anterior

Os quatro endpoints são expostos por `PedidoController`, cujo caminho-base é `/pedidos`.

| Operação | Parâmetros | Corpo da requisição | Sucesso | Respostas de erro |
|---|---|---|---|---|
| POST /pedidos | Nenhum parâmetro de rota | PedidoRequest | 201 Created | 400, 404, 422 |
| GET /pedidos/{id} | id na rota (Long) | Nenhum | 200 OK | 404 |
| POST /pedidos/{id}/itens | id na rota (Long) | ItemPedidoRequest | 200 OK | 400, 404, 422 |
| POST /pedidos/{id}/confirmar | id na rota (Long) | Nenhum | 200 OK | 404, 422 |

### DTOs e validações existentes

Os DTOs estão definidos em ApiDtos.java:

- PedidoRequest: clienteId (@NotNull), restauranteId (@NotNull), enderecoEntregaId (@NotNull) e itens (@NotEmpty, com validação em cascata por @Valid).
- ItemPedidoRequest: produtoId (@NotNull) e quantidade (@Min(1)).
- PedidoResponse: id, clienteId, restauranteId, enderecoEntregaId, itens, subtotal, taxaEntrega, valorTotal, distanciaEntregaKm e status.
- ItemResponse: produtoId, nome, quantidade, precoUnitario e subtotal.
- ErroResponse: status, erro e mensagem.

O parâmetro id é recebido como Long por @PathVariable. No controller original não há anotação como @Positive ou @Min que imponha o limite mínimo 1.

### Tratamento de erros

ApiExceptionHandler mapeia:

- HTTP 400, código VALIDATION_ERROR: erros de Bean Validation.
- HTTP 404, código NOT_FOUND: RecursoNaoEncontradoException.
- HTTP 422, código REGRA_NEGOCIO: RegraNegocioException, IllegalArgumentException e IllegalStateException.

### Testes existentes relacionados

- FoodNowFlowIT: executa o fluxo de criação, inclusão de item e confirmação.
- FoodNowErrorsIT: verifica cenários de erro.
- GeografiaContratoIT: compara respostas HTTP com arquivos JSON de referência.
- ApiTestSupport: fornece configuração e cenários para os testes.
- PedidoTest: contém testes unitários da entidade Pedido.

Não foi identificado no repositório um frontend, aplicativo cliente, SDK ou outro serviço de produção que consuma os endpoints de pedidos. Os consumidores identificados diretamente no repositório são os testes de integração que exercitam a API HTTP.

O contrato anterior está documentado em:

docs/openapi/foodnow-before-observacao.yaml

## 2. Contrato proposto para a observação

O novo campo é observacao, do tipo string, opcional e com tamanho máximo de 500 caracteres.

Ele é recebido na criação por PedidoRequest e disponibilizado em PedidoResponse, utilizado nas quatro operações analisadas.

A validação por @Size(max = 500) estabelece o limite máximo sem tornar o campo obrigatório.

Política definida:

| Entrada | Comportamento |
|---|---|
| Propriedade ausente | Aceita; valor fica null |
| null explícito | Aceito; equivale à ausência |
| String vazia ("") | Aceita e armazenada como string vazia |
| De 1 a 500 caracteres | Aceita |
| 501 caracteres | Rejeitada com HTTP 400 |

Quando observacao é null, a propriedade é omitida do JSON de resposta. Quando o valor é uma string vazia ou possui conteúdo, a propriedade aparece na resposta.

A entidade Pedido armazena o campo em coluna com tamanho máximo de 500 caracteres. O valor é recebido na criação e disponibilizado por getter. Não foi criado setter nem endpoint para edição posterior.

O serviço mantém a assinatura anterior de criação como delegação para a nova assinatura, preservando chamadas existentes que não informem observação.

A inclusão de itens e a confirmação operam sobre o mesmo pedido persistido e preservam a observação recebida na criação.

A especificação atualizada está em:

docs/openapi/foodnow.yaml

## 3. Consumidores e compatibilidade

### Consumidores identificados no repositório

- FoodNowFlowIT, FoodNowErrorsIT e GeografiaContratoIT consomem a API HTTP por meio do Rest Assured.
- ApiTestSupport prepara dados e cenários por meio de chamadas HTTP.
- GeografiaContratoIT compara respostas completas com arquivos de referência.
- Não foi identificado consumidor de produção, como frontend, aplicativo, SDK ou serviço externo.

### Consumidores hipotéticos

- Aplicativo web ou mobile de clientes;
- Painel de atendimento ou administração;
- Integração de restaurante/parceiro;
- SDK ou serviço de terceiro que desserialize PedidoResponse.

Esses consumidores são possibilidades, não componentes encontrados no repositório.

### Classificação e decisão de versionamento

A evolução é aditiva e compatível sob condições, não uma garantia universal de compatibilidade.

1. A nova propriedade de entrada é opcional. Clientes antigos que não a enviam continuam podendo criar pedidos.
2. A ausência e o null mantêm o comportamento anterior.
3. Quando não existe observação, a propriedade é omitida na resposta.
4. Quando existe observação, a resposta passa a conter uma propriedade adicional. Consumidores que ignoram campos desconhecidos tendem a continuar funcionando; consumidores com desserialização estrita, validação rígida de schema ou comparação integral do JSON podem precisar de adaptação.

Não se propõe uma rota /v2, pois o campo de entrada é opcional, os clientes antigos continuam funcionando e não foi identificado no repositório um consumidor externo estrito que exija outro contrato.

Essa decisão pressupõe que consumidores externos tolerem propriedades adicionais nas respostas quando a observação estiver preenchida. Se essa premissa não for verdadeira para algum consumidor real, a compatibilidade deverá ser reavaliada.

## 4. Comparação antes/depois

| Elemento | Contrato anterior | Contrato proposto |
|---|---|---|
| PedidoRequest | Não possui observacao | Campo opcional, string, máximo 500 caracteres |
| Entidade Pedido | Não armazena observação | Armazena observação em coluna de até 500 caracteres |
| PedidoResponse | Não possui observacao | Disponibiliza observação quando houver valor; omite quando null |
| Criação sem observação | Aceita | Continua aceita |
| Inclusão de item | Retorna dados existentes | Preserva e retorna a observação original |
| Confirmação | Retorna dados existentes | Preserva e retorna a observação original |
| Edição da observação | Não aplicável | Não permitida |
| Versionamento | Rotas existentes | Mesmas rotas, sem /v2 |

## 5. Testes adicionados

FoodNowPedidoObservacaoIT cobre:

- Criação com observação e recuperação por GET /pedidos/{id};
- Preservação da observação ao adicionar item;
- Preservação da observação ao confirmar;
- Criação sem a propriedade;
- Envio explícito de null;
- Envio de string vazia;
- Aceitação de exatamente 500 caracteres;
- Rejeição de 501 caracteres com HTTP 400.

Os testes existentes foram mantidos.

A execução informada de mvn clean verify terminou com BUILD SUCCESS, com 29 testes unitários e 17 testes de integração aprovados, sem falhas ou erros.

## 6. Cobertura e validação

A cobertura de linhas informada pelo relatório JaCoCo foi de 97,78%, acima do mínimo exigido de 80%.

O relatório HTML é gerado em:

target/site/jacoco/index.html

A validação semântica das duas especificações OpenAPI ainda deve ser registrada como evidência no Pull Request, por exemplo por meio de captura do Swagger Editor.

Comando de verificação executado:

mvn clean verify