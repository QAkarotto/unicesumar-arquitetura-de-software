# Atividade 02 — 2º bimestre: Dependency Rule e Limites Arquiteturais

Integrante: Pedro Terra (`PedroTerra11`).

## Dependência escolhida e problema

A dependência escolhida foi a autorização de pagamentos. Na base do professor, commit `c8eab129588411c8abe0b87cfc21a6ed4e1b3d68`, `PagamentoService` recebia diretamente `FakePaymentGateway` no construtor. Além de coordenar o caso de uso, o serviço montava `GatewayRequest`, conhecia coordenadas no formato textual do provedor e interpretava `providerStatus` comparando com `AUTHORIZED`.

```text
Antes: PagamentoService → FakePaymentGateway
```

As classes envolvidas eram `PagamentoService`, `FakePaymentGateway` e seus records `GatewayRequest` e `GatewayResult`, além de `Localizacao`, `FormaPagamento` e `StatusPagamento`. O provedor também convertia o valor monetário para centavos a partir da solicitação construída pelo serviço.

Uma troca de provedor com outros nomes de status ou formatos de entrada exigiria alterar o serviço que também registra o pagamento, notifica o cliente e solicita a entrega. A injeção por construtor já existia, mas recebia a classe concreta; por isso, sozinha, não eliminava o conhecimento dos detalhes externos.

## Limite adotado

Foi criado o contrato `AutorizadorPagamento` no pacote `pagamento`, pertencente à aplicação. A solicitação usa `BigDecimal`, `FormaPagamento` e `Localizacao`. A resposta usa `StatusPagamento`, código e mensagem. Nenhum desses tipos de transferência depende do gateway ou do Spring, nem transporta a entidade JPA inteira.

```text
PagamentoService → AutorizadorPagamento ← AutorizadorPagamentoAdapter → FakePaymentGateway
```

O serviço depende apenas do contrato para obter uma autorização. `AutorizadorPagamentoAdapter`, no pacote de integração, implementa esse contrato e traduz a solicitação para `GatewayRequest`. Também traduz o resultado para o status da aplicação. A conversão do valor em centavos continua no método `GatewayRequest.from`, chamado agora pelo adaptador.

O adaptador mantém a interpretação anterior: `AUTHORIZED` corresponde a `APROVADO`; os demais status correspondem a `REJEITADO`. Não foi introduzida uma nova política para respostas desconhecidas, pois isso mudaria o comportamento em uma atividade de refatoração.

O Spring injeta o adaptador pelo construtor do serviço. Como ele é o único componente que implementa a interface, não foi necessário criar configuração adicional. A solução acompanha o limite de localização já presente na base, com `ProvedorLocalizacao` e `MapsLocalizacaoAdapter`, sem reorganizar os demais fluxos.

## Alterações e limites

| Arquivo | Responsabilidade |
| --- | --- |
| `pagamento/AutorizadorPagamento.java` | Interface e records de solicitação e resultado, no vocabulário da aplicação |
| `integration/AutorizadorPagamentoAdapter.java` | Tradução dos formatos e do status do provedor |
| `service/PagamentoService.java` | Utilização do contrato em lugar do gateway concreto |
| `service/PagamentoServiceTest.java` | Testes do caso de uso sem o gateway ou adaptador |
| `integration/AutorizadorPagamentoAdapterTest.java` | Testes da tradução entre o contrato e o provedor |

O cliente simulado permanece intacto, incluindo a consulta de transações. Essa consulta não foi acrescentada à nova interface porque o serviço não a utiliza: `PagamentoService.consultar` lê o repositório local. O contrato contém somente a operação necessária neste limite.

Os endpoints, DTOs HTTP, entidades, repositórios, valores calculados, notificações e critérios de entrega não foram alterados. A ordem de persistência, notificação e criação da entrega foi preservada. O projeto continua com integrações locais simuladas; esta entrega não acrescenta retry, tratamento de indisponibilidade ou processamento assíncrono.

Esta branch parte diretamente da `master` do professor e contém somente a Atividade 02. As alterações de observação da Atividade 01 permanecem em seu PR próprio.

## Testes e evidências

`PagamentoServiceTest` instancia o serviço com mocks dos colaboradores, incluindo a interface `AutorizadorPagamento`. Ele não importa classes de `integration`, não inicia o Spring e não utiliza banco ou provedor externo. O mesmo token de teste produz aprovação ou rejeição conforme o resultado fornecido pelo mock, demonstrando que o caso de uso não depende do token especial do simulador.

Os testes verificam as duas decisões de negócio, dados persistidos, encaminhamento da solicitação, estado final do pedido, notificação, entrega somente quando aprovado e ordem dos efeitos. Também verificam que um pedido inexistente ou não confirmado não chega ao autorizador.

`AutorizadorPagamentoAdapterTest` verifica a conversão do valor para centavos, forma de pagamento, região, coordenadas e referência do pedido, além da tradução de aprovação, rejeição e status desconhecido. Código e mensagem do provedor permanecem disponíveis na resposta interna.

Todos os testes de API anteriores, incluindo `GeografiaContratoIT` e seus arquivos de referência, foram mantidos sem mudanças. Esses testes conferem o comportamento HTTP e os valores retornados usando a integração real entre os componentes simulados.

O resultado do `mvn clean verify` está em [validacao.md](validacao.md).

## Direção das dependências após a refatoração

As dependências de código passaram a apontar para o contrato da aplicação: o serviço utiliza `AutorizadorPagamento`, e o adaptador externo implementa essa interface. O fluxo de execução ainda chega ao gateway, mas o serviço não o conhece. Isso permite substituir o provedor ou seu protocolo modificando o adaptador, com menor impacto nas regras de pagamento, e testar o caso de uso sem a implementação externa.

A contrapartida é manter uma interface, seus tipos e uma tradução adicional. Esse custo fica limitado à fronteira de pagamento; não se pretende afirmar que todo o FoodNow passou a seguir Clean Architecture.

## Fundamentação

- Robert C. Martin, [The Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html): dependências voltadas às políticas internas e tradução dos formatos externos na fronteira.
- Spring Framework, [Dependency Injection](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html): colaboradores recebidos por construtor, permitindo substituições em testes.
- Robert C. Martin, *Clean Architecture: A Craftsman's Guide to Software Structure and Design*, Pearson, 2017: referência bibliográfica indicada no enunciado; [catálogo e sumário da editora](https://www.pearson.com/en-us/subject-catalog/p/clean-architecture-a-craftsmans-guide-to-software-structure-and-design/P200000009528/9780134494166).
