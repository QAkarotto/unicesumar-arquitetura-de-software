# Atividade 06 — Organização do código de entregas no FoodNow

**Integrante(s):** [PREENCHER]

## O que foi analisado

Analisei os pontos do projeto que trabalham com endereço, distância, região, taxa, prazo e entregas. O levantamento de 10 pontos e o fluxo **criar → confirmar → pagar → entregar** estão explicados em `ENTREGA_ATIVIDADE_06.md`.

## Mudança feita neste pacote

A base que utilizei **já estava parcialmente refatorada** conforme o material de referência, inclusive com separação de logística, mapas e notificações. Minha contribuição adicional foi isolar o simulador de entregadores: `EntregaService` passou a depender de `ProvedorEntregadores`, enquanto `FakeCourierAdapter` é responsável por conversar com `FakeCourierClient`. Também incluí testes para situações normais e falta de entregador.

A ideia foi fazer uma alteração limitada, sem mudar regras, endpoints ou retornos da API. A interface acrescenta arquivos, mas evita que o serviço principal dependa diretamente da implementação do simulador.

## Validação

Resultado registrado no terminal do Windows em 16/10/2026, **antes do último ajuste de nome de variável**:

- `mvn clean verify` — **BUILD SUCCESS**
- 32 testes unitários + 11 testes de integração
- 0 falhas, 0 erros, 0 testes ignorados
- JaCoCo: **96% de cobertura de instruções** no relatório exibido; verificações de cobertura do Maven aprovadas

**Pendente:** executar novamente `mvn clean verify` após o ajuste final de nomenclatura e registrar o resultado no PR.

## Limites

Não foi implementada a regra futura de horário. Não foram modificados o contrato HTTP, os DTOs, a persistência ou as regras de negócio. Os pontos que ficaram fora do escopo estão detalhados na documentação.

**Apoio utilizado:** IA para auxiliar na refatoração, testes e organização do relatório; material de referência disponibilizado pelo professor.
