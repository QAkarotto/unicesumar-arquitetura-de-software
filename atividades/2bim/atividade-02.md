# Atividade 02 - 2BIM: Dependency Rule e Limites Arquiteturais no FoodNow

## Objetivo

Analisar uma dependência do projeto **FoodNow** e realizar uma refatoração localizada aplicando os conceitos de **Dependency Rule, limites arquiteturais e inversão de dependência**.

## Contexto

Em uma aplicação, regras de negócio frequentemente precisam utilizar recursos externos, como banco de dados, APIs, serviços de pagamento, mapas ou notificações.

O problema não está em utilizar esses recursos, mas em **como as dependências são organizadas**.

Na Clean Architecture, a *Dependency Rule* estabelece que dependências de código devem apontar para as regras e abstrações mais estáveis da aplicação. Dessa forma, regras de negócio não precisam conhecer diretamente detalhes como uma biblioteca, um framework ou uma implementação específica de integração.

No FoodNow existem integrações e elementos de infraestrutura que permitem analisar esse tipo de dependência.

## Pesquisa

Antes de alterar o projeto, consulte os materiais sugeridos e procure compreender:

- **Clean Architecture e Dependency Rule**  
  Robert C. Martin apresenta a ideia de que as dependências devem apontar para as políticas mais internas, enquanto detalhes de infraestrutura permanecem nas regiões externas da arquitetura.  
  https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html

- **Clean Architecture: A Craftsman's Guide to Software Structure and Design**  
  Livro de Robert C. Martin, especialmente os capítulos relacionados a componentes, limites e Dependency Rule.  
  https://www.pearson.com/en-us/subject-catalog/p/clean-architecture-a-craftsmans-guide-to-software-structure-and-design/P200000009528/9780134494166

- **Dependency Injection no Spring**  
  A documentação do Spring explica como objetos podem receber suas dependências sem precisar criá-las ou localizá-las diretamente.  
  https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html

Não é necessário implementar toda a Clean Architecture no projeto. O objetivo é compreender e aplicar esses conceitos em **um limite específico** do FoodNow.

## Atividade

1. Escolha **uma dependência de infraestrutura ou integração externa** existente no FoodNow, como localização/mapas, pagamento, notificações ou persistência.

2. Localize onde essa implementação é utilizada e analise a direção atual das dependências. Registre no Pull Request as classes envolvidas e explique por que a estrutura atual pode dificultar a substituição, manutenção ou teste dessa integração.

3. Proponha um limite entre a regra da aplicação e o detalhe externo. A regra de negócio deve depender de um contrato ou abstração apropriada, e não diretamente da implementação escolhida.

4. Implemente uma **refatoração localizada**. Você pode introduzir interfaces ou outras abstrações quando fizer sentido, mas não é necessário reorganizar todo o projeto ou aplicar uma arquitetura específica.

5. Preserve o comportamento atual do FoodNow. A troca da estrutura interna não deve modificar o funcionamento observado pelos consumidores da aplicação.

6. Crie ou ajuste testes unitários para demonstrar que a regra da aplicação pode ser testada sem depender diretamente da implementação externa escolhida.

7. Execute `mvn clean verify` e confirme que todos os testes continuam passando e que a cobertura mínima do projeto foi mantida.

## Exemplo de análise

Considere uma regra de pedido que depende diretamente de uma classe específica de mapas.

Antes:

```text
PedidoService → FakeMapsClient
```

Uma possível análise seria questionar:

> Se o FoodNow substituir esse cliente de mapas por outro provedor, quais classes precisarão ser alteradas?

A refatoração deve buscar uma estrutura na qual a regra da aplicação conheça **o que precisa ser feito**, mas não necessariamente **qual tecnologia ou implementação executa essa operação**.

O exemplo serve apenas para ilustrar o problema. Não é obrigatório utilizar localização, nem seguir uma solução específica.

## Restrições

- Não altere os endpoints ou contratos existentes da API.
- Não altere os testes de API para adequá-los à refatoração.
- Não remova funcionalidades existentes.
- Não é necessário aplicar Clean Architecture em todo o FoodNow.
- Os testes de API devem continuar funcionando sem modificações.

## Entrega

A atividade pode ser realizada **individualmente ou em dupla**.

A entrega deverá ser feita por **Pull Request** contendo:

- identificação dos integrantes;
- dependência escolhida;
- breve análise do problema encontrado;
- decisão adotada;
- principais alterações realizadas;
- resultado do `mvn clean verify`.

Inclua também uma resposta curta para:

> **Após a refatoração, em qual direção as dependências passaram a apontar e o que isso permite modificar com menor impacto?**

O foco da atividade está na **direção das dependências e na definição de um limite arquitetural**, e não na quantidade de código alterado.
