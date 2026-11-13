# Atividade 02 - 2BIM: Dependency Rule e Limites Arquiteturais no FoodNow

## Integrantes

- Danilo Bolzan de Moraes
- Edi Carlos Francisco Junior

## Dependência escolhida

Integração de pagamento com o `FakePaymentGateway`.

## Análise do problema

Antes da refatoração, `PagamentoService` importava diretamente `FakePaymentGateway`,
criava `FakePaymentGateway.GatewayRequest` e interpretava o texto `AUTHORIZED`
de `FakePaymentGateway.GatewayResult`. Assim, o caso de uso conhecia tanto a classe
concreta quanto o protocolo particular do provedor.

Direção anterior:

`PagamentoService -> FakePaymentGateway`

Essa direção fazia uma troca de provedor exigir mudanças na regra de aplicação.

## Decisão arquitetural

Foi criada a fronteira `ProcessadorPagamento`, pertencente à aplicação, acompanhada
dos contratos `SolicitacaoPagamento` e `ResultadoPagamento`. O
`GatewayPagamentoAdapter` implementa essa interface e concentra a tradução entre o
modelo estável da aplicação e os tipos específicos de `FakePaymentGateway`.

Direção após a refatoração:

`PagamentoService -> ProcessadorPagamento <- GatewayPagamentoAdapter -> FakePaymentGateway`

## Principais mudanças

- `PagamentoService` passou a depender da abstração `ProcessadorPagamento`.
- Tipos aninhados do gateway deixaram de aparecer no caso de uso.
- A interpretação do status textual do provedor foi movida para o adapter.
- O comportamento externo, os endpoints e os testes de API foram preservados.
- Testes unitários verificam o serviço com uma implementação controlada da fronteira
  e a tradução realizada pelo adapter.

## Resposta

Após a refatoração, as dependências passaram a apontar dos detalhes externos para o
contrato definido pela aplicação. Isso permite substituir ou alterar o gateway de
pagamento e seu protocolo com menor impacto, mantendo a regra de negócio e os
contratos HTTP estáveis.

## Verificação

O comando `mvn clean verify` foi executado com sucesso:

- 33 testes unitários aprovados;
- 11 testes de integração/API aprovados;
- nenhuma falha, erro ou teste ignorado;
- 97,94% de cobertura de linhas, acima do mínimo de 80%.
