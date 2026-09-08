# Atividade 04: Decisão Arquitetural e Trade-offs

## Objetivo

Analisar um sistema de software a partir de **requisitos arquiteturalmente significativos**, **atributos de qualidade** e **trade-offs**, justificando uma decisão arquitetural com base no contexto.

## Atividade

Escolha **um sistema real ou conhecido**, como e-commerce, banco digital, streaming, delivery, rede social, sistema hospitalar ou plataforma educacional.

Realize a seguinte análise:

### Contexto

Descreva brevemente:

- o sistema escolhido;
- seus principais usuários;
- o problema que ele resolve.

### Requisitos Arquiteturalmente Significativos

Identifique **3 requisitos** que possam influenciar diretamente a arquitetura do sistema.

Exemplos:

- suportar muitos usuários simultâneos;
- proteger dados sensíveis;
- permanecer disponível diante de falhas;
- integrar-se com sistemas externos;
- responder rapidamente às requisições;
- permitir evolução frequente.

### Atributos de Qualidade

Relacione cada requisito a um atributo de qualidade relevante, como:

- desempenho;
- segurança;
- disponibilidade;
- escalabilidade;
- manutenibilidade;
- testabilidade;
- interoperabilidade.

### Priorização

Escolha **um atributo de qualidade prioritário** para o sistema e justifique a escolha.

### Decisão Arquitetural

Proponha **uma decisão arquitetural** relacionada ao atributo priorizado e explique:

- qual problema busca resolver;
- qual benefício oferece;
- qual custo, limitação ou complexidade introduz.

Esse último ponto representa o principal **trade-off** da decisão.

## Exemplo

**Sistema:** plataforma de streaming.

**Requisito:** suportar grandes picos de usuários simultâneos.

**Atributo:** escalabilidade.

**Decisão:** permitir escalabilidade horizontal dos componentes responsáveis pela entrega de conteúdo.

**Benefício:** aumento da capacidade conforme a demanda.

**Trade-off:** maior complexidade operacional e custo de infraestrutura.

## Organização

A atividade pode ser realizada **individualmente ou em dupla**.

Em caso de dupla, os dois integrantes devem estar identificados na entrega.

## Entrega

A atividade deve ser entregue por meio de **uma Issue no GitHub Projects da disciplina**:

https://github.com/users/QAkarotto/projects/9

- Individual: criar uma Issue com a própria análise.
- Dupla: criar uma única Issue e identificar os dois integrantes.
- Organizar a resposta utilizando os tópicos apresentados nesta atividade.
- Priorizar respostas objetivas e justificadas.

Não é necessário conhecer a arquitetura interna real do sistema. Quando necessário, podem ser utilizadas **hipóteses fundamentadas** nas características observáveis do sistema.
