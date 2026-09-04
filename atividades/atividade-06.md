# Atividade 06: Modularidade, Coesão e Acoplamento no FoodNow

## Objetivo

Analisar e refatorar responsabilidades relacionadas a localização e entrega no
FoodNow, reduzindo o acoplamento e melhorando a coesão sem alterar o
comportamento observável da aplicação.

## Contexto

O projeto possui regras de distância, região, área de atendimento, taxa e tempo
de entrega distribuídas entre entidades, services, utilitários e integrações.
Também existem cálculos diferentes para conceitos semelhantes.

Considere o seguinte requisito futuro:

> A taxa de entrega deverá considerar a distância do trajeto, a região, o
> horário e outras condições operacionais.

Esse requisito deve ser utilizado apenas para analisar o impacto de mudanças.
**Não implemente a nova regra nesta atividade.** Os resultados atuais devem ser
preservados.

## Atividade

1. Identifique pelo menos **8 pontos do código** relacionados a localização,
   distância, região, atendimento, taxa ou tempo de entrega. O levantamento
   deve abranger ao menos quatro tipos de componentes: domínio, services,
   utilitários, integrações, API ou persistência.

2. Para cada ponto, informe:
   - classe e método;
   - responsabilidade encontrada;
   - dependências envolvidas;
   - problema arquitetural;
   - impacto provável de uma mudança.

3. Represente as principais dependências do fluxo:

   ```text
   criar pedido -> confirmar -> pagar -> criar/consultar entrega
   ```

4. Proponha uma nova distribuição de responsabilidades e implemente uma
   refatoração que inclua a criação do pedido e pelo menos um fluxo relacionado,
   como pagamento, notificação ou entrega.

5. A refatoração deve demonstrar:
   - redução das responsabilidades de pelo menos uma classe;
   - concentração de pelo menos três regras ou cálculos dispersos;
   - redução da dependência direta de uma integração geográfica concreta;
   - um limite claro para a futura regra de rota, região e horário.

6. Compare a estrutura antes e depois da refatoração, indicando os componentes
   afetados pelo requisito futuro, as dependências reduzidas e os trade-offs da
   solução.

Não é necessário corrigir todos os problemas do projeto. Os problemas mantidos
fora do escopo devem ser registrados e justificados.

## Preservação de comportamento

Para as mesmas entradas, devem permanecer inalterados:

- endpoints, métodos HTTP, códigos de status e campos JSON;
- regras de aceitação e rejeição de pedidos;
- valores de subtotal, taxa, total, distância e tempo;
- regiões, zonas, riscos e dados de despacho;
- transições de status de pedido, pagamento e entrega;
- notificações enviadas.

Resultados distintos expostos em campos diferentes da API também fazem parte
do comportamento atual e devem ser preservados.

## Testes e restrições

- Execute `mvn clean verify` antes e depois da refatoração.
- Não modifique nem remova os testes de API existentes.
- Adicione testes de caracterização quando o comportamento escolhido não
  estiver suficientemente protegido.
- Testes unitários podem ser ajustados à nova estrutura, desde que continuem
  protegendo o mesmo comportamento.
- Mantenha cobertura mínima de **80% de linhas**, sem novas exclusões no JaCoCo.
- Não utilize serviços externos reais, API keys ou microsserviços.
- Não implemente o requisito futuro de horário.

## Organização

A atividade pode ser realizada **individualmente ou em dupla**.

## Entrega

A entrega deverá ser realizada por **Pull Request** contendo:

- integrantes;
- problemas identificados e dependências do fluxo;
- decisão arquitetural e escopo adotado;
- principais alterações e testes realizados;
- comparação antes/depois;
- problemas não tratados e trade-offs;
- resultado do `mvn clean verify` e cobertura obtida.

O Pull Request deve apresentar mudanças efetivas na distribuição de
responsabilidades e dependências. Apenas mover classes ou alterar packages não
é suficiente.
