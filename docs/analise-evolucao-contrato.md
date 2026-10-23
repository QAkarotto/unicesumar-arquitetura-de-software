# Evolução do contrato de pedidos — Atividade 01 / 2º bimestre

## Escopo e referências

Trabalho na branch `8sfelipe-atividade01-2bim`. A base anterior é o commit
`710207a67efa5b7254b1a2cd4920f14372b2f9b3`. O contrato anterior está em
[openapi-before.yaml](openapi-before.yaml); o atualizado, em
[openapi-after.yaml](openapi-after.yaml). As versões `1.0.0-before` e
`1.1.0-after` identificam documentos, sem criar versões de rota.

Fontes da implementação, sob `src/main/java/br/edu/foodnow/`:

- `controller/PedidoController.java`: `criar`, `consultar`, `adicionarItem`, `confirmar`.
- `controller/ApiDtos.java`: `PedidoRequest`, `ItemPedidoRequest`, `PedidoResponse`, `ItemResponse`, `ErroResponse` e conversores `from`.
- `controller/ApiExceptionHandler.java`: `validacao`, `naoEncontrado`, `regraNegocio`.
- `controller/ObservacaoDeserializer.java`: `deserialize`, restrito ao novo campo.
- `service/PedidoService.java`: `criar`, `consultar`, `adicionarItem`, `adicionarItemCarregado`, `confirmar`.
- `model/Pedido.java`: construção, `adicionarItem`, `definirEntrega`, `recalcularValores`, `confirmar` e `getObservacao`.
- `model/ItemPedido.java`: construção e `calcularSubtotal`.
- `repository/PedidoRepository.java`: `JpaRepository<Pedido, Long>`, com `save` e `findById` herdados.
- `logistica/LogisticaService.java` e `PoliticaEntregaAtual.java`: cotação e adicional dos itens.
- `service/NotificacaoService.java`: `notificarConfirmacao`.

## As quatro operações

| Operação | Entrada | Sucesso | Erros identificados |
|---|---|---|---|
| `POST /pedidos` | Corpo obrigatório `PedidoRequest`, validado com `@Valid` | 201, `PedidoResponse`, status `CRIADO` | 400 por validação/leitura; 404 para cliente, restaurante ou produto inexistente; 422 por regra de negócio |
| `GET /pedidos/{id}` | `id` de rota obrigatório, Java `Long`, sem validação de positividade | 200, `PedidoResponse` | 404 pedido inexistente; 400 de conversão do parâmetro pelo framework |
| `POST /pedidos/{id}/itens` | `id` e corpo obrigatório `ItemPedidoRequest` com `@Valid` | 200, `PedidoResponse` | 400 por validação/leitura/conversão; 404 pedido ou produto inexistente; 422 por regra de negócio |
| `POST /pedidos/{id}/confirmar` | `id`; nenhum corpo declarado | 200, `PedidoResponse`, status `CONFIRMADO` | 400 de conversão; 404 pedido inexistente; 422 sem itens ou status diferente de `CRIADO` |

Não há parâmetros de consulta ou cabeçalhos específicos declarados. A criação
não constrói `Location`. Os controllers não restringem explicitamente
`consumes`/`produces`; a especificação representa o uso JSON dos testes.

Na criação, o endereço precisa pertencer ao cliente. A cotação exige área de
entrega válida e também considera o endereço principal do cliente. A adição
verifica disponibilidade, possibilidade de entrega e restaurante do produto.
A confirmação exige itens e status `CRIADO`, notifica e salva.

Regras preservadas: itens repetidos viram novas linhas; a adição não verifica
status nem refaz a cotação logística. Subtotal e total seguem os cálculos
anteriores. Pagamento e entrega não receberam alterações.

## DTOs e erros

`PedidoRequest` exige `clienteId`, `restauranteId`, `enderecoEntregaId`
(`Long`, `@NotNull`) e `itens` (`@NotEmpty List<@Valid ItemPedidoRequest>`).
Não há restrição de positividade dos identificadores nem `@NotNull` sobre
cada elemento da lista. Um elemento nulo não constitui item utilizável e pode
causar falha não tratada ao ser acessado pelo controller.

`ItemPedidoRequest` exige `produtoId` (`Long`, `@NotNull`) e quantidade
válida (`int`, `@Min(1)`). Continua sem campo de observação.

`PedidoResponse` retorna `id`, `clienteId`, `restauranteId`,
`enderecoEntregaId`, `itens`, `subtotal`, `taxaEntrega`, `valorTotal`,
`distanciaEntregaKm`, `status` e agora `observacao`. `ItemResponse` retorna
`produtoId`, `nome`, `quantidade`, `precoUnitario` e `subtotal`.
Valores monetários são `BigDecimal` e números JSON. Os estados possíveis são
`CRIADO`, `CONFIRMADO`, `PAGO`, `PAGAMENTO_REJEITADO`, `EM_ENTREGA`, `ENTREGUE`.

`ErroResponse` contém `status`, `erro`, `mensagem`. O handler mapeia:

- `MethodArgumentNotValidException`: 400 / `VALIDATION_ERROR`; primeiro erro de campo, com mensagem dependente do locale.
- `RecursoNaoEncontradoException`: 404 / `NOT_FOUND`.
- `RegraNegocioException`, `IllegalArgumentException`, `IllegalStateException`: 422 / `REGRA_NEGOCIO`.

Falhas de leitura JSON e conversão de parâmetros não possuem handler próprio;
continuam tratadas pelo Spring MVC. Os testes de tipos inválidos verificam
400 sem inventar um formato estável de corpo para esses erros.

## Comparação antes/depois e valores opcionais

| Aspecto | Antes | Depois |
|---|---|---|
| Entrada de criação | Sem propriedade declarada para observação | `observacao` opcional, `String`, `@CodePointLength(max = 500)` |
| Campo ausente ou `null` | Sem armazenamento específico | Armazena `null`; propriedade omitida na resposta |
| String vazia | Sem armazenamento específico | Aceita, armazena e devolve `""` |
| Espaços | Sem armazenamento específico | Aceita sem `trim`; conta no limite e preserva o conteúdo |
| 500 pontos de código Unicode | Sem limite específico para observação | Aceitos e persistidos |
| 501 pontos de código Unicode | Sem limite específico para observação | 400 / `VALIDATION_ERROR`, antes de persistir |
| Número, booleano, objeto, array | Sem propriedade tipada para observação | 400 de desserialização, sem coerção para string |
| Respostas de pedidos | Campos anteriores | Campo adicional se não nulo, nos quatro endpoints |
| Edição | Nenhuma operação específica | Nenhuma operação, setter ou caminho de atualização da observação |

O limite segue `@CodePointLength(max = 500)` do Hibernate Validator, já
disponível na dependência de validação do projeto. São contados pontos de
código Unicode, de acordo com `maxLength: 500` do OpenAPI. Um emoji
suplementar conta como um ponto de código, mesmo ocupando duas unidades
UTF-16. Não há normalização: sequências combinantes e emojis compostos
podem conter vários pontos de código, independentemente da aparência visual.

O desserializador atua somente em `PedidoRequest.observacao`, preservando as
conversões dos campos antigos. `PedidoController.criar` encaminha o valor para
`PedidoService.criar`, que o passa ao construtor da entidade. O construtor
anterior foi mantido e delega com `null`, preservando os usos internos existentes.

Na entidade, `@Column(length = 1000, updatable = false)` reserva espaço para
até 1000 unidades UTF-16, comportando 500 pontos de código suplementares no
H2. O limite da API permanece 500 pontos de código, validado no DTO; o tamanho
físico da coluna não aumenta esse limite. A coluna é anulável e não participa
de updates JPA. Não há setter. A API só aceita o valor na criação; enviar essa propriedade
às operações de itens ou confirmação não a edita, conforme teste automatizado.
O banco continua H2 em memória com `create-drop`: persistência entre requisições,
sem promessa de durabilidade após reiniciar a aplicação. Não houve migração
porque a configuração existente recria o esquema.

O arquivo principal configura `spring.jackson.default-property-inclusion=non_null`.
O arquivo de propriedades de testes não repete essa configuração. Por isso,
o novo componente de `PedidoResponse` também declara explicitamente
`@JsonInclude(NON_NULL)`, garantindo a mesma omissão nos dois contextos, sem
alterar configurações e sem modificar as referências JSON antigas.

## Consumidores e compatibilidade

Consumidores HTTP encontrados no repositório:

- `src/test/java/br/edu/foodnow/controller/FoodNowFlowIT.java`: `deveExecutarFluxoCompletoDoPedidoAteAEntrega`.
- `src/test/java/br/edu/foodnow/controller/FoodNowErrorsIT.java`: casos de rejeição, pedido sem itens e fluxo de pagamento rejeitado.
- `src/test/java/br/edu/foodnow/controller/GeografiaContratoIT.java`: os quatro endpoints e comparação exata do JSON com `src/test/resources/geografia/*.properties`.
- `src/test/java/br/edu/foodnow/controller/ApiTestSupport.java`: montagem de cenários e `criarPedido` via HTTP.
- Novo `src/test/java/br/edu/foodnow/controller/PedidoObservacaoIT.java`: testes específicos da evolução.

Consumidores internos da entidade incluem pagamento, entrega, logística,
notificação e os testes de domínio. Integrações `Fake*` são simuladores
chamados pelo backend, não evidência de aplicativos externos consumindo pedidos.
README e enunciados são documentação, não clientes executáveis.

Consumidores hipotéticos, **não encontrados**: aplicativo móvel, frontend web,
sistemas parceiros, SDKs gerados e validadores externos com schemas fechados.

A mudança é aditiva e compatível para clientes que enviam o contrato anterior:
os campos antigos continuam obrigatórios como antes e pedidos sem observação
não ganham uma chave nula na resposta. As comparações exatas de geografia
continuam válidas sem alterações. Clientes que enviam a nova informação
recebem a nova propriedade nos quatro endpoints.

Não se afirma compatibilidade universal: clientes rígidos que rejeitam campos
adicionais podem precisar atualizar seu schema quando consumirem pedidos com
observação, inclusive se criados por outro cliente. Quem já enviava uma
propriedade desconhecida chamada `observacao` com tipo inválido passa a ter
essa propriedade reconhecida e validada. Esses casos exigem coordenação com
consumidores reais caso apareçam; não há evidência deles no repositório.

Não foi criada `/v2`: não houve remoção ou mudança de significado dos campos,
rotas ou status anteriores; o novo campo é opcional e a omissão preserva o
fluxo antigo. O impacto condicionado de schemas fechados está explicitado,
sem presumir que todos os consumidores externos sejam tolerantes.

## Verificação

Os testes anteriores foram mantidos sem alteração. `PedidoObservacaoIT`
verifica texto, ausência, `null`, vazio, espaços, 500/501 pontos de código,
incluindo emojis, caracteres suplementares não emoji e textos mistos, seis
casos de valores JSON inválidos, leitura direta da coluna por JDBC, consulta por GET e
manutenção após adicionar itens e confirmar, além de tentativas de edição
pelas duas operações posteriores.

Os comandos executados, resultados finais e cobertura estão registrados no
[resumo para o Pull Request](pr-description.md). Não foram feitas mudanças
no `pom.xml`, nas configurações, nas exclusões JaCoCo ou nas referências dos
testes existentes. O documento anterior permanece preservado.
