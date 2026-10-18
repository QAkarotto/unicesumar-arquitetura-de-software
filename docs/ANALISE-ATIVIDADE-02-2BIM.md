# Atividade 02 do 2º bimestre (Dependency Rule e limites arquiteturais)

Bryan Charles e Guilherme Sartori

## Dependência escolhida

Escolhemos o pagamento, ou seja, o `FakePaymentGateway` usado pelo `PagamentoService`.

Mapas já tinha passado por uma refatoração parecida na base do professor, com o
`ProvedorLocalizacao` e o `MapsLocalizacaoAdapter`. O e-mail é injetado pelo `ApiTestSupport`
como `FakeEmailClient` concreto, e mexer nele obrigaria a alterar os testes de API, o que a
atividade não permite. O pagamento era a integração que mais misturava regra da aplicação com
detalhe do provedor e que dava para isolar sem tocar em nenhum teste de API.

## Análise do problema

Antes da refatoração a dependência apontava da regra para o detalhe.

```
PagamentoController -> PedidoService -> PagamentoService -> FakePaymentGateway
                                                            (GatewayRequest, GatewayResult)
```

| Classe | Papel |
|---|---|
| `service.PagamentoService` | caso de uso de pagamento. Valida o pedido, cobra, registra o `Pagamento`, notifica e cria a entrega |
| `integration.FakePaymentGateway` | provedor simulado, com os records `GatewayRequest` e `GatewayResult` |
| `model.Localizacao` | fornece `formatarParaProvedor`, usado para montar a requisição |
| `model.StatusPagamento` | status da aplicação, `APROVADO` ou `REJEITADO` |

O `PagamentoService` importava o `FakePaymentGateway` e conhecia o protocolo dele por inteiro.
Montava o `GatewayRequest` com o id do pedido como texto, o valor em centavos e as coordenadas no
formato de texto do provedor. Depois comparava `providerStatus` com a string `"AUTHORIZED"` para
decidir se o pagamento estava aprovado.

Isso atrapalha de três jeitos.

Para **substituir** o provedor, o caso de uso precisa ser alterado. Um provedor real teria
outro formato de requisição, outros códigos de status e possivelmente erros de comunicação, e
tudo isso ia parar dentro do `PagamentoService`, que também cuida de notificação e entrega.

Para **manter**, uma mudança no protocolo do provedor, como renomear um status ou mudar a
unidade do valor, vira uma mudança na regra de pagamento, mesmo que a regra continue igual.

Para **testar**, o caso de uso só funcionava com o gateway concreto. O resultado dependia do
token mágico `REJEITADO`, e para simular outra resposta do provedor seria preciso mexer na
classe do fake ou usar mocks de classe concreta presos ao formato dele.

## Decisão adotada

Criamos um limite entre o caso de uso e o provedor, com a abstração do lado da aplicação.

```
PagamentoService -> ProcessadorPagamento <- GatewayPagamentoAdapter -> FakePaymentGateway
   (service)          (pagamento)              (integration)              (integration)
```

`ProcessadorPagamento` é uma interface no pacote novo `pagamento`, junto com os records
`SolicitacaoPagamento` e `ResultadoPagamento`. Ela fala a língua da aplicação, com
`BigDecimal`, `FormaPagamento`, `Localizacao` e `StatusPagamento`, e não sabe nada de centavos,
coordenadas em texto ou `"AUTHORIZED"`.

`GatewayPagamentoAdapter`, no pacote `integration`, implementa a interface e é o único lugar que
conhece o `FakePaymentGateway`. Ele traduz a solicitação para o `GatewayRequest` e o
`GatewayResult` de volta para um `ResultadoPagamento`.

A injeção de dependência do Spring liga as duas pontas. O `PagamentoService` pede um
`ProcessadorPagamento` no construtor e o Spring entrega o adaptador, que é o único bean que
implementa a interface. Nenhuma configuração extra foi necessária.

Seguimos o mesmo padrão que o professor usou para localização, com a porta perto da regra e o
adaptador em `integration`, para o projeto não ficar com dois estilos diferentes para o mesmo
problema.

Ficaram de fora o `EntregaService`, que ainda depende do `FakeCourierClient`, e o
`NotificacaoService`, que ainda depende do `FakeEmailClient`. A atividade pede uma refatoração
localizada, e no caso do e-mail mudar o tipo injetado quebraria o `ApiTestSupport`.

## Principais alterações

| Arquivo | Mudança |
|---|---|
| `pagamento/ProcessadorPagamento.java` | novo, interface do limite |
| `pagamento/SolicitacaoPagamento.java` | novo, o que o caso de uso pede ao provedor |
| `pagamento/ResultadoPagamento.java` | novo, o que o caso de uso precisa saber da resposta |
| `integration/GatewayPagamentoAdapter.java` | novo, traduz entre a porta e o `FakePaymentGateway` |
| `service/PagamentoService.java` | passa a depender de `ProcessadorPagamento`. Saíram o import do gateway, a montagem do `GatewayRequest` e a comparação com `"AUTHORIZED"` |
| `service/PagamentoServiceTest.java` | novo, testa o caso de uso com um processador falso |
| `integration/GatewayPagamentoAdapterTest.java` | novo, testa a tradução feita pelo adaptador |

O `FakePaymentGateway` não foi alterado, e a ordem das chamadas dentro do `processar` continua
a mesma.

## Testes

O `PagamentoServiceTest` não usa o `FakePaymentGateway` nem o adaptador. Ele passa para o
`PagamentoService` um `ProcessadorFalso`, uma implementação de `ProcessadorPagamento` escrita no
próprio teste que guarda as solicitações e devolve a resposta que o teste escolher. Os
repositórios e os outros serviços são mocks do Mockito, que já vem no `spring-boot-starter-test`.

| Teste | O que mostra |
|---|---|
| `deveRegistrarPagamentoAprovadoComODadoDevolvidoPeloProcessador` | aprovação vira pedido `PAGO`, código e mensagem vêm do processador, há notificação e a entrega é criada |
| `deveRegistrarRejeicaoSemCriarEntrega` | rejeição vira `PAGAMENTO_REJEITADO` e nenhuma entrega é criada |
| `deveEnviarAoProcessadorOsDadosDoPedidoNoVocabularioDaAplicacao` | a solicitação leva valor, forma, token, região e as localizações do pedido |
| `naoDeveChamarProcessadorParaPedidoNaoConfirmado` | pedido fora de `CONFIRMADO` é barrado antes de chegar ao provedor |
| `deveRejeitarPedidoInexistenteSemChamarProcessador` | pedido inexistente dá `RecursoNaoEncontradoException` sem chamar o provedor |

Antes, o resultado do pagamento dependia do token `REJEITADO` do fake. Agora o teste decide a
resposta diretamente, sem token mágico, e poderia simular qualquer outro resultado que um
provedor real devolvesse.

O `GatewayPagamentoAdapterTest` cobre o outro lado do limite. Ele confere que a solicitação
vira um `GatewayRequest` com `4230` centavos e coordenadas `-25.100000,-50.150000`, e que
`AUTHORIZED` e `DECLINED` viram `APROVADO` e `REJEITADO`.

Os testes de API não foram alterados. O `GeografiaContratoIT` compara o JSON completo da resposta
de pagamento contra snapshots, incluindo `codigoExterno` e `mensagemProvedor`, e continuou
passando. Isso mostra que o comportamento visto pelos consumidores não mudou.

## Resultado do `mvn clean verify`

JDK 26 (Temurin 26.0.2) e Maven 3.9.9.

```
mvn clean verify  ->  BUILD SUCCESS
Testes unitários  37, falhas 0   (antes 29)
Testes de API     11, falhas 0   (antes 11, sem alteração)
All coverage checks have been met.
```

| Contador | Antes | Depois |
|---|---|---|
| Linhas | 700/715, 97,90% | 710/724, 98,07% |
| Branches | 107/132, 81,06% | 108/132, 81,82% |
| Instruções | 3738/3867, 96,66% | 3823/3942, 96,98% |

Nenhuma exclusão nova no JaCoCo.

## Para onde as dependências apontam agora

Agora as dependências apontam para dentro, na direção da regra. O `PagamentoService` depende só
da interface `ProcessadorPagamento`, que pertence à aplicação, e o detalhe externo também passou
a depender dela, porque o `GatewayPagamentoAdapter` a implementa. Nenhuma classe de `service`
importa mais nada de pagamento do pacote `integration`. Com isso dá para trocar o provedor de
pagamento, mudar o protocolo dele ou tratar erros de comunicação mexendo só no adaptador, ou
criando outro adaptador. A regra de pagamento, a notificação, a entrega e a API continuam como
estão, e o caso de uso pode ser testado com qualquer implementação falsa da interface.
