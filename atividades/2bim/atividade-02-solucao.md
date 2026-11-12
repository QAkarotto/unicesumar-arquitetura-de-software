# Atividade 02 – 2BIM: Dependency Rule e Limites Arquiteturais no FoodNow

**Aluno:** Guilherme Augusto Eidam
**RA:** 23167609-2
**Projeto:** FoodNow
**Atividade:** Dependency Rule e Limites Arquiteturais

## 1. Dependência escolhida

A dependência escolhida para a refatoração foi a integração de envio de notificações por e-mail, representada originalmente pela classe `FakeEmailClient`, localizada no pacote `br.edu.foodnow.integration`.

Essa dependência era utilizada diretamente pela classe `NotificacaoService`, responsável por enviar notificações de confirmação de pedido, início de entrega e resultado do pagamento.

## 2. Análise do problema encontrado

Antes da refatoração, a classe `NotificacaoService` importava diretamente `FakeEmailClient` e utilizava essa implementação concreta como dependência em seu construtor.

A estrutura anterior era:

```text
NotificacaoService
       |
       v
FakeEmailClient
```

Essa organização criava um acoplamento entre a regra de aplicação e um detalhe de infraestrutura.

Caso fosse necessário substituir o mecanismo de envio de e-mails por outro provedor, a classe `NotificacaoService` precisaria conhecer a nova implementação. Isso aumentaria o impacto das mudanças e dificultaria a realização de testes unitários isolados.

## 3. Decisão arquitetural

Foi introduzida a interface `EmailGateway`, no pacote `br.edu.foodnow.notificacao`, para representar o contrato de envio de notificações por e-mail.

A classe `FakeEmailClient` passou a implementar essa interface, mantendo sua responsabilidade de simular o envio e armazenar as notificações em memória.

A classe `NotificacaoService` passou a receber `EmailGateway` por injeção de dependência no construtor, deixando de depender diretamente da implementação concreta.

A estrutura após a refatoração ficou:

```text
NotificacaoService
       |
       v
  EmailGateway
       ^
       |
 FakeEmailClient
```

Com isso, o serviço conhece apenas as operações necessárias para realizar o envio, sem conhecer os detalhes da implementação utilizada.

## 4. Principais alterações realizadas

### 4.1. Criação da interface EmailGateway

Foi criada a interface `EmailGateway`, contendo os métodos:

- `enviar(String destinatario, String assunto, String mensagem)`;
- `enviar(Notificacao notificacao)`.

Essa interface representa o limite arquitetural entre a regra da aplicação e o mecanismo de envio de e-mails.

### 4.2. Adaptação de FakeEmailClient

A classe `FakeEmailClient` passou a implementar `EmailGateway`.

A implementação original foi preservada, incluindo os métodos `getEnviadas()` e `limpar()`, mantendo o funcionamento dos testes e das funcionalidades existentes.

### 4.3. Refatoração de NotificacaoService

A propriedade e o construtor da classe foram alterados para receber `EmailGateway`, em vez de `FakeEmailClient`.

As operações de confirmação de pedido, início de entrega e notificação de pagamento foram mantidas, preservando o comportamento anterior.

### 4.4. Teste unitário independente

Foi criada a classe `NotificacaoServiceTest`, utilizando uma implementação em memória da interface `EmailGateway`.

O teste verifica o envio de uma notificação de início de entrega, validando destinatário, assunto e mensagem.

Dessa forma, foi possível testar a regra de aplicação sem instanciar diretamente a classe `FakeEmailClient`.

## 5. Preservação do comportamento

A refatoração foi localizada e não modificou os endpoints, DTOs ou contratos existentes da API.

As notificações continuam sendo enviadas pelas mesmas operações e com as mesmas informações.

A implementação simulada continua sendo utilizada pelo Spring por meio da injeção de dependência, e os testes de integração existentes continuam funcionando.

## 6. Testes e validação

Foi executado o comando:

```bash
mvn clean verify
```

Resultado obtido:

| Verificação | Resultado |
|---|---:|
| Testes unitários | 30 aprovados |
| Testes de integração | 11 aprovados |
| Total | 41 aprovados |
| Falhas | 0 |
| Erros | 0 |
| Cobertura JaCoCo | Todos os limites atendidos |
| Build | BUILD SUCCESS |

O novo teste `NotificacaoServiceTest` foi aprovado, assim como os testes existentes do projeto.

## 7. Resposta: direção das dependências

Após a refatoração, as dependências passaram a apontar para uma abstração, representada pela interface `EmailGateway`, em vez de apontarem diretamente para a implementação concreta `FakeEmailClient`.

A regra da aplicação passou a depender do contrato, enquanto a implementação de infraestrutura passou a implementar essa abstração.

Isso permite substituir o mecanismo de envio de e-mails com menor impacto sobre as regras de negócio, além de facilitar os testes unitários sem depender diretamente de uma integração externa.

## 8. Conclusão

A refatoração demonstrou a aplicação prática da Dependency Rule e da inversão de dependência em um limite específico do FoodNow.

A introdução da interface `EmailGateway` reduziu o acoplamento entre a classe `NotificacaoService` e a implementação de infraestrutura.

O comportamento existente foi preservado e os testes comprovaram que a alteração não comprometeu as funcionalidades anteriores.

Assim, o projeto passou a permitir maior flexibilidade na substituição das implementações de envio de e-mails, mantendo as regras da aplicação independentes dos detalhes de infraestrutura.