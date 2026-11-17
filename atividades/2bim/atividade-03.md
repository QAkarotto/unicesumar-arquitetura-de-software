## Atividade 03 – Integrações Externas e Tratamento de Falhas

**Integrante:** Ramon Albini Vieira e Eduardo Rodrigues

### Integração escolhida

Serviço de mapas e localização, por meio de `FakeMapsClient` e `MapsLocalizacaoAdapter`.

### Cenários de falha identificados

1. Indisponibilidade temporária do provedor, causando falha na consulta de coordenadas ou rotas.
2. Timeout persistente, no qual o provedor continua indisponível após novas tentativas.
3. Resposta inválida, com coordenadas em formato incompatível com o esperado pela aplicação.

### Decisão adotada

Foi implementada uma política de retry limitado no `MapsLocalizacaoAdapter`.

A aplicação realiza até três tentativas para operações que lançam `FalhaTemporariaMapsException`, com intervalos crescentes de 50 ms e 100 ms.

Falhas não classificadas como transitórias não são repetidas.

Quando todas as tentativas falham, a exceção é propagada.

### Benefício

A solução permite recuperar operações de consulta após falhas temporárias do provedor, reduzindo interrupções causadas por indisponibilidades momentâneas.

### Trade-off

As novas tentativas aumentam a latência das operações. Caso o serviço permaneça indisponível, a falha continua sendo propagada após o limite configurado.

A estratégia é adequada às operações de consulta de mapas, que não produzem alterações de estado no provedor.

### Validação

- Comando: `mvn clean verify`
- Cobertura JaCoCo: Cobertura de linhas - 97.30%