# Atividade 01 – 2BIM: APIs como Fronteiras Arquiteturais

**Aluno:** Guilherme Augusto Eidam
**RA:** 23167609-2
**Projeto:** FoodNow
**Atividade:** APIs como Fronteiras Arquiteturais

## 1. Objetivo

A atividade teve como objetivo analisar o contrato da API de pedidos do FoodNow e implementar a inclusão de um campo opcional chamado `observacao` na criação de pedidos.

A alteração permite que o cliente informe uma observação com até 500 caracteres. Essa informação é armazenada junto ao pedido e disponibilizada nas respostas da API que representam esse pedido.

A implementação preserva os endpoints existentes, mantém a compatibilidade com requisições antigas e não permite a edição da observação depois da criação.

## 2. Análise do contrato original

Antes da implementação, foram documentadas as operações existentes por meio do arquivo:

`docs/openapi/pedidos-v1-original.yaml`

Foram analisados os seguintes endpoints:

| Método | Endpoint | Finalidade | Sucesso |
|---|---|---|---|
| POST | `/pedidos` | Criar pedido | 201 |
| GET | `/pedidos/{id}` | Consultar pedido | 200 |
| POST | `/pedidos/{id}/itens` | Adicionar item | 200 |
| POST | `/pedidos/{id}/confirmar` | Confirmar pedido | 200 |

O contrato original já exigia os campos `clienteId`, `restauranteId`, `enderecoEntregaId` e `itens` para criar um pedido.

As operações também possuíam respostas de erro para situações como dados inválidos, recursos não encontrados e violações de regras de negócio.

## 3. Alteração proposta no contrato

Foi incluído o campo `observacao` no objeto `PedidoRequest`.

As características definidas são:

- Campo opcional, não sendo necessário enviá-lo para criar um pedido.
- Limite máximo de 500 caracteres.
- Aceita valor `null`.
- Texto vazio ou composto apenas por espaços é tratado como ausência de observação.
- A observação é armazenada junto ao pedido.
- Não existe operação para alterar a observação depois da criação.

O campo também foi incluído no objeto `PedidoResponse`. Dessa maneira, quando houver uma observação registrada, ela estará disponível nas respostas de criação, consulta, inclusão de itens e confirmação.

Quando não existir observação, a propriedade não será incluída no JSON de resposta.

## 4. Comparação entre o contrato original e o atualizado

| Elemento | Contrato original | Contrato atualizado |
|---|---|---|
| POST `/pedidos` | Não recebe observação | Recebe `observacao` opcional |
| GET `/pedidos/{id}` | Retorna os dados do pedido | Também retorna a observação, quando existente |
| POST `/pedidos/{id}/itens` | Retorna os dados do pedido | Preserva e retorna a observação |
| POST `/pedidos/{id}/confirmar` | Retorna os dados do pedido | Preserva e retorna a observação |
| Limite de caracteres | Não aplicável | 500 caracteres |
| Edição posterior | Não havia campo | Continua não permitida |

O contrato original foi preservado no arquivo `docs/openapi/pedidos-v1-original.yaml`.

A nova especificação está disponível em `docs/openapi/pedidos-v1-atualizado.yaml`.

## 5. Compatibilidade e consumidores da API

A alteração foi implementada de maneira aditiva, sem modificar os caminhos, métodos HTTP ou códigos de resposta dos endpoints existentes.

As requisições antigas continuam funcionando porque o campo `observacao` não é obrigatório. Assim, os consumidores que ainda não enviam essa propriedade não precisam modificar suas requisições.

Entretanto, a inclusão de uma propriedade em respostas JSON exige atenção. Consumidores que utilizam desserialização rígida e rejeitam propriedades desconhecidas podem precisar de ajustes para aceitar o novo campo.

Para reduzir esse risco, recomenda-se que os aplicativos consumidores aceitem propriedades adicionais nas respostas da API.

Os consumidores relevantes incluem:

- Aplicação do cliente, que poderá informar observações durante a criação do pedido.
- Aplicações de restaurante e entregador, caso consumam as respostas que representam pedidos.
- Testes automatizados de integração, responsáveis por verificar os contratos e o fluxo da API.

No código do projeto, também foram considerados os testes existentes `FoodNowFlowIT`, `FoodNowErrorsIT` e `GeografiaContratoIT`, além do suporte de testes HTTP em `ApiTestSupport`.

## 6. Estratégia de versionamento

Foi mantida a versão `1.0.0` da API.

A decisão foi baseada no fato de que a alteração não removeu propriedades existentes, não modificou os endpoints e não tornou obrigatório nenhum novo campo nas requisições.

A inclusão de uma propriedade opcional é uma evolução aditiva do contrato. Portanto, não foi necessário criar uma nova rota ou uma versão `v2`.

Apesar disso, consumidores que rejeitam propriedades desconhecidas devem ser avaliados antes da implantação. Caso algum consumidor essencial não consiga aceitar essa evolução e não seja possível atualizá-lo de forma coordenada, uma estratégia de versionamento com novo contrato deverá ser considerada.

## 7. Implementação realizada

A implementação foi distribuída entre as seguintes classes:

**`Pedido.java`**

Foi incluído o atributo `observacao`, com tamanho máximo de 500 caracteres e mapeamento de persistência. A normalização trata valores nulos e textos vazios ou compostos apenas por espaços. Não foi criado um método setter para permitir alteração posterior.

**`ApiDtos.java`**

O DTO `PedidoRequest` passou a receber a propriedade `observacao`, com validação de tamanho máximo de 500 caracteres.

O DTO `PedidoResponse` também passou a disponibilizar a observação. A anotação `JsonInclude.Include.NON_NULL` evita que a propriedade seja serializada quando não houver valor, preservando o formato das respostas antigas nessa situação.

Foram mantidos construtores compatíveis com os argumentos anteriores dos DTOs.

**`PedidoService.java`**

Foi criada uma sobrecarga do método de criação do pedido para receber a observação. A assinatura anterior foi preservada e delega para a nova implementação, passando `null` quando a observação não for informada.

**`PedidoController.java`**

O controlador passou a encaminhar a observação recebida na requisição ao serviço responsável pela criação do pedido.

As demais operações continuam utilizando o mesmo fluxo de consulta, inclusão de itens e confirmação.

## 8. Testes realizados

Foi criada a classe `PedidoObservacaoIT`, contendo seis testes de integração para verificar:

1. Criação de pedido com observação e recuperação da informação após consulta, inclusão de item e confirmação.
2. Criação sem informar o campo `observacao`.
3. Criação com `observacao` igual a `null`.
4. Tratamento de texto vazio e composto apenas por espaços.
5. Aceitação de uma observação com exatamente 500 caracteres.
6. Rejeição de uma observação com 501 caracteres, retornando HTTP 400.

Também foram executados os testes existentes do projeto para garantir que as funcionalidades anteriores continuassem funcionando.

**Resultado da execução de `mvn clean verify`:**

| Verificação | Resultado |
|---|---:|
| Testes unitários | 29 aprovados |
| Testes de integração | 17 aprovados |
| Total de testes | 46 aprovados |
| Falhas | 0 |
| Erros | 0 |
| Verificação JaCoCo | Todos os limites atendidos |
| Build | BUILD SUCCESS |

## 9. Documentação e validação OpenAPI

Foram criados dois arquivos para registrar a evolução do contrato:

- `docs/openapi/pedidos-v1-original.yaml`
- `docs/openapi/pedidos-v1-atualizado.yaml`

A especificação atualizada