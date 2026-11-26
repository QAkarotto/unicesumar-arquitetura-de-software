# Arquitetura de Software: 30 questões de estudo

Responda de forma breve e justifique as decisões quando solicitado.

## Decisões arquiteturais e requisitos

### Questão 1
O que caracteriza uma decisão arquitetural? Apresente um exemplo.

### Questão 2
Por que uma arquitetura adequada para uma empresa pode ser inadequada para outra? Cite três fatores que influenciam essa escolha.

### Questão 3
Diferencie requisito funcional de atributo de qualidade usando dois exemplos de um sistema de pedidos.

### Questão 4
O que torna um requisito arquiteturalmente significativo? Ele precisa ser necessariamente não funcional?

### Questão 5
Uma equipe pequena precisa lançar um produto com orçamento limitado. Compare uma solução simples com outra preparada para uma demanda muito superior à atual e recomende uma delas.

### Questão 6
O que é um trade-off arquitetural? Explique um benefício e um custo de manter instâncias redundantes de uma aplicação.

## Atributos de qualidade

### Questão 7
Diferencie desempenho, escalabilidade e elasticidade.

### Questão 8
Um sistema deve responder rapidamente, restringir acessos indevidos, continuar operando durante falhas e facilitar novas integrações. Relacione cada exigência a um atributo de qualidade.

### Questão 9
Um sistema permanece acessível, mas perde registros já confirmados após uma falha. Explique por que disponibilidade não é suficiente nesse caso.

### Questão 10
Um sistema de avaliações precisa atender muitos alunos simultaneamente, preservar respostas e limitar o acesso às provas. Identifique três requisitos relevantes e seus atributos de qualidade.

### Questão 11
Uma plataforma de pagamentos tem recursos limitados. Priorize dois atributos de qualidade e justifique as escolhas com base nos riscos do negócio.

### Questão 12
Escolha uma decisão arquitetural que favoreça os atributos priorizados na questão anterior. Explique um benefício e um trade-off.

## Coesão, acoplamento e modularidade

### Questão 13
Uma classe calcula preços, envia e-mails, gera relatórios e acessa o banco. Analise sua coesão e proponha uma melhoria.

### Questão 14
Diferencie coesão de acoplamento. Por que ambos precisam ser analisados na organização do software?

### Questão 15
Uma mudança interna em Clientes exige alterações em Pedidos e Pagamentos. Que problema isso pode indicar? Como reduzir seu impacto?

### Questão 16
Por que organizar classes nos pacotes controller, service, repository e model não garante boa modularidade?

### Questão 17
Como organizar um sistema de delivery por funcionalidades de negócio? Cite três módulos e explique como suas responsabilidades seriam delimitadas.

### Questão 18
Quatro módulos reproduzem a regra de cálculo do prazo de entrega. Quais problemas isso pode causar e onde essa regra deveria ficar?

### Questão 19
Como contratos explícitos permitem a comunicação entre módulos sem expor todos os seus detalhes internos?

### Questão 20
Criar uma interface entre dois módulos elimina o acoplamento? Justifique.

## Arquitetura web

### Questão 21
Quais são as responsabilidades do frontend, do backend e da persistência em uma aplicação web?

### Questão 22
Um frontend conhece as tabelas do banco e executa SQL diretamente. Explique dois problemas dessa organização e proponha uma alternativa.

## Monólito modular e microsserviços

### Questão 23
Diferencie monólito modular de microsserviços quanto à implantação e à comunicação entre as partes.

### Questão 24
Um sistema fica lento nos horários de pico. Quais informações devem ser levantadas antes de propor uma mudança arquitetural?

### Questão 25
A lentidão está concentrada em uma consulta sem índice adequado. A equipe implanta os módulos juntos e não precisa escalá-los separadamente. Qual seria uma ação inicial fundamentada? Justifique.

### Questão 26
Em que condições a adoção de microsserviços pode ser justificada? Apresente dois benefícios potenciais e dois custos.

## Representações arquiteturais

### Questão 27
Por que gestores, desenvolvedores e programadores podem precisar de representações diferentes do mesmo sistema? Indique o foco de cada público.

### Questão 28
Relacione cada representação a um nível do modelo C4: usuários e sistemas externos; aplicação web, API e banco; componentes internos da API; classes e interfaces.

### Questão 29
Por que um diagrama de classes pode ser inadequado como ponto de partida para uma reunião sobre orçamento e integração com parceiros? Qual representação seria mais útil?

### Questão 30
Qual é a diferença entre mostrar a estrutura de um sistema e mostrar a sequência de interações de uma operação? Dê um exemplo de informação apresentada em cada caso.
