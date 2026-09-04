# Atividade 06: Modularidade, Coesão e Acoplamento no FoodNow

## Objetivo

Analisar e refatorar uma parte coerente das responsabilidades relacionadas a
**localização, área de atendimento, cálculo de distância, taxa e entrega** no
projeto FoodNow.

A refatoração deverá reduzir o acoplamento e melhorar a coesão sem alterar o
comportamento observável da aplicação.

## Contexto

O FoodNow funciona, possui testes automatizados e está organizado em camadas
técnicas. Entretanto, regras e dados relacionados à localização participam de
várias funcionalidades, como cadastro, catálogo, pedido, pagamento,
notificação e entrega.

O sistema também possui mais de uma maneira de:

- calcular ou validar uma distância;
- definir uma região ou zona de entrega;
- verificar se um endereço é atendido;
- formar uma taxa ou custo relacionado à entrega;
- estimar o tempo de entrega;
- consumir dados de uma integração simulada.

Algumas dessas diferenças fazem parte do comportamento atual da aplicação e
podem aparecer nas respostas da API. Portanto, refatorar não significa
simplesmente substituir todos os resultados por uma única fórmula.

## Cenário de mudança arquitetural

Considere o seguinte requisito **futuro**:

> O FoodNow passará a utilizar uma nova estratégia para localização e taxa de
> entrega. A taxa deverá considerar a distância do trajeto, a região de
> entrega, o horário e outras condições operacionais.

Esse requisito é um instrumento para analisar o impacto de mudança antes e
depois da refatoração.

**Não implemente a nova regra de rota, região e horário nesta atividade.** A
implementação atual e seus resultados devem ser preservados. O objetivo é
preparar melhor a estrutura para que uma mudança desse tipo fique mais
localizada no futuro.

## Atividade

### 1. Reconhecimento do problema

Identifique pelo menos **8 pontos do código** que possuam responsabilidades ou
dependências relacionadas a localização, distância, região, área atendida,
taxa ou tempo de entrega.

O levantamento deverá abranger pelo menos **quatro tipos de componentes** entre:

- entidades ou objetos do domínio;
- services;
- utilitários;
- integrações simuladas;
- controllers ou DTOs;
- persistência.

Para cada ponto, registre:

| Responsabilidade encontrada | Local atual | Dependências envolvidas | Problema arquitetural | Possível impacto de mudança |
|---|---|---|---|---|
| Exemplo identificado pelo grupo | Classe e método | Elementos dos quais depende | Coesão, acoplamento, duplicação etc. | Classes ou fluxos afetados |

O grupo também deverá representar, por diagrama simples ou lista encadeada, as
dependências percorridas pelo fluxo principal:

```text
criar pedido -> confirmar -> pagar -> criar/consultar entrega
```

Não basta listar nomes de classes. A análise deve explicar por que a
responsabilidade está mal distribuída ou por que a dependência dificulta uma
mudança.

### 2. Caracterização do comportamento atual

Antes da refatoração:

1. execute `mvn clean verify`;
2. registre o resultado e a cobertura inicial;
3. identifique quais testes protegem o trecho escolhido;
4. crie testes de caracterização quando o comportamento relevante ainda não
   estiver suficientemente protegido.

Os testes de caracterização devem verificar resultados e mudanças de estado,
sem fixar ordem de chamadas ou a organização interna das classes.

### 3. Proposta arquitetural

Proponha uma distribuição mais clara das responsabilidades encontradas. A
proposta deverá indicar:

- qual será o limite da refatoração;
- quais responsabilidades terão um proprietário mais claro;
- como os dados da integração simulada serão tratados;
- quais dependências deixarão de existir ou mudarão de direção;
- quais problemas identificados permanecerão fora do escopo;
- quais trade-offs foram aceitos.

Não existe uma única organização correta. A solução pode manter camadas
técnicas, adotar organização por funcionalidade ou combinar as duas
abordagens, desde que a decisão seja justificada.

### 4. Refatoração

Refatore uma **fatia vertical coerente** que inclua obrigatoriamente o cálculo
usado na criação do pedido e pelo menos um fluxo relacionado, como simulação de
entrega, pagamento, notificação ou criação da entrega.

A solução deverá demonstrar, de forma verificável:

- redução de responsabilidades de pelo menos uma classe excessivamente
  carregada;
- eliminação ou centralização de pelo menos três regras ou cálculos que estavam
  distribuídos;
- redução de dependência direta de algum componente da fatia escolhida em uma
  implementação concreta de integração geográfica;
- existência de um ponto claro para introduzir futuramente dados de rota,
  região e horário;
- manutenção dos resultados atuais para as mesmas entradas.

Não é necessário corrigir todos os problemas arquiteturais do FoodNow. Os
problemas que permanecerem deverão ser indicados explicitamente na entrega.

### 5. Análise de impacto

Compare o cenário futuro de rota, região e horário antes e depois da
refatoração.

Apresente:

- componentes que seriam afetados antes;
- componentes que seriam afetados depois;
- dependências removidas ou invertidas;
- responsabilidades que passaram a possuir um limite mais claro;
- limitações e novos trade-offs da solução.

Uma redução apenas na quantidade de arquivos modificados não é suficiente. A
comparação deve explicar por que o novo limite reduz o acoplamento ou melhora a
coesão.

## Comportamento que deve ser preservado

Para as mesmas requisições e dados de entrada, preserve:

- paths, métodos HTTP, códigos de status e campos JSON de todos os endpoints;
- criação e consulta de clientes, restaurantes e produtos;
- resultados dos endpoints de simulação de atendimento e entrega;
- aceitação ou rejeição de endereços na criação do pedido;
- subtotal, taxa, total, distância e status do pedido;
- confirmação do pedido;
- aprovação e rejeição de pagamento;
- região e risco geográfico registrados no pagamento;
- criação da entrega somente após pagamento aprovado;
- distância, zona, estimativa, despacho e transições de status da entrega;
- notificações atualmente enviadas em cada etapa.

Quando um endpoint expõe cálculos diferentes em campos distintos, os campos e
seus respectivos resultados continuam fazendo parte do contrato observável.

## Restrições

- Não altere os endpoints nem os contratos de requisição e resposta.
- Não modifique nem remova os testes de API já existentes.
- É permitido adicionar novos testes de API.
- Testes unitários podem ser criados ou ajustados para acompanhar novos limites
  internos, mas não devem perder as verificações de comportamento existentes.
- Não reduza a cobertura por meio de exclusões adicionais no JaCoCo.
- A cobertura mínima de **80% de linhas** deve ser mantida.
- Não utilize serviços externos reais nem adicione dependência de internet ou
  API keys.
- Não transforme o projeto em microsserviços.
- Não crie interfaces, factories ou strategies sem uma responsabilidade ou
  variação concreta que justifique sua existência.
- Não implemente o requisito futuro de horário.

Valide a solução executando:

```bash
mvn clean verify
```

## Critérios de aceitação

A atividade será considerada concluída quando:

- o levantamento apresentar os pontos e tipos de componentes solicitados;
- a proposta e o código implementado forem coerentes entre si;
- a fatia refatorada incluir criação de pedido e outro fluxo relacionado;
- houver evidência objetiva de redução de responsabilidade, duplicação e
  dependência concreta;
- o comportamento observável listado nesta atividade for preservado;
- os testes de API originais estiverem inalterados e passando;
- `mvn clean verify` terminar com sucesso;
- a cobertura de linhas permanecer em pelo menos 80%;
- a análise antes/depois utilizar o cenário de rota, região e horário;
- os problemas deixados fora do escopo e os trade-offs forem documentados.

## Organização

A atividade pode ser realizada **individualmente ou em dupla**.

## Entrega

A entrega deverá ser realizada por **Pull Request** contendo:

- nomes dos integrantes;
- tabela de problemas identificados;
- representação do fluxo e das dependências atuais;
- limite e decisão arquitetural adotados;
- principais alterações realizadas;
- testes de caracterização adicionados ou ajustados;
- comparação antes/depois para o cenário futuro;
- problemas que permaneceram fora do escopo;
- trade-offs da solução;
- resultado do `mvn clean verify` e cobertura obtida.

O Pull Request não deve conter apenas movimentação de arquivos ou alteração de
packages. A melhoria deverá estar refletida na distribuição das
responsabilidades e nas dependências do código.
