# Atividade 02 — Dependency Rule 

## Integração analisada

A integração escolhida é o gateway de pagamento simulado `FakePaymentGateway`, o qual representa um provedor externo: recebe os dados de uma cobrança e devolve o resultado da autorização. A simulação aprova qualquer token não vazio, exceto `REJEITADO`, preservando o comportamento documentado do FoodNow.

## Classes envolvidas no fluxo

| Classe | Responsabilidade |
|---|---|
| `PagamentoController` | Recebe a chamada HTTP e delega o processamento ao serviço. O contrato HTTP permanece inalterado. |
| `PagamentoService` | Coordena a regra de pagamento: valida o pedido, solicita autorização, registra o resultado, notifica e inicia a entrega quando aprovado. |
| `GatewayPagamento` | Contrato pertencente à aplicação para solicitar um pagamento e receber um resultado independente do provedor. |
| `FakePaymentGateway` | Adaptador de integração. Implementa o contrato da aplicação e converte os dados e estados da simulação. |
| `GatewayRequest` e `GatewayResult` | Estruturas próprias do gateway simulado; permanecem encapsuladas em `FakePaymentGateway`. |
| `PedidoRepository` e `PagamentoRepository` | Carregam e persistem as entidades envolvidas. |
| `Pedido` e `Pagamento` | Representam o pedido e o registro de pagamento no domínio. |
| `LocalizacaoService` | Calcula os dados de rota/região usados na cobrança e no registro do pagamento. |
| `NotificacaoService` e `EntregaService` | Executam os efeitos posteriores ao resultado do pagamento. |

## Direção das dependências antes da refatoração

Antes desta alteração, `PagamentoService` importava diretamente `FakePaymentGateway`, recebia a implementação concreta no construtor, montava `FakePaymentGateway.GatewayRequest` e interpretava o texto `AUTHORIZED` de `FakePaymentGateway.GatewayResult`. O serviço de aplicação conhecia tanto a integração escolhida quanto o vocabulário e os formatos internos dela.


Essa direção dificulta substituir o provedor: uma integração nova com DTOs, códigos de retorno ou formato monetário diferentes exigiria mudanças dentro do serviço que coordena a regra. Também torna mais difícil testar esse serviço isoladamente com uma implementação controlada, pois seu construtor exige a classe concreta.

## Limite proposto e refatoração aplicada

Foi introduzido `GatewayPagamento` em `service`, no lado da aplicação. O contrato define `RequisicaoPagamento` e `ResultadoPagamento`; o resultado usa `aprovado` como valor tipado, sem expor códigos textuais do provedor. `PagamentoService` depende apenas desse contrato.

`FakePaymentGateway` agora implementa `GatewayPagamento` e converte a requisição da aplicação para seus tipos internos. A tradução de `AUTHORIZED` para `aprovado=true` ocorre dentro do adaptador. 
