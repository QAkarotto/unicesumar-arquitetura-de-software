# Atividade 04 — Decisão Arquitetural e Trade-offs

**Integrante(s):** Pedro [+ nome da dupla, se aplicável]

**Sistema escolhido:** FoodNow — plataforma de delivery de alimentos

---

## 1. Contexto

A FoodNow é uma plataforma de delivery de alimentos composta por três aplicações: um app para o cliente final, um portal para o restaurante parceiro e um app para o entregador, todos consumindo uma API REST comum.

- **Principais usuários:** clientes que querem pedir comida; restaurantes parceiros que administram cardápio e recebem pedidos; entregadores que executam a logística de entrega.
- **Problema que resolve:** conecta a demanda (clientes com fome, em um local específico) à oferta (restaurantes próximos com capacidade de atender), e coordena em tempo quase real a produção do pedido, o pagamento e a entrega física até o cliente.

## 2. Requisitos Arquiteturalmente Significativos (RAS)

| # | Requisito |
|---|---|
| RAS 1 | Suportar picos de usuários simultâneos em horários concentrados de demanda (almoço e jantar) sem degradar o tempo de resposta. |
| RAS 2 | Permanecer disponível mesmo diante de falhas parciais — um pedido com entregador já a caminho não pode ser perdido se uma parte do sistema falhar. |
| RAS 3 | Integrar-se de forma confiável com sistemas externos essenciais ao fluxo do pedido (PayPal para pagamento, Google Maps Platform para geolocalização e rotas). |

## 3. Atributos de Qualidade

| Requisito | Atributo de qualidade relacionado |
|---|---|
| RAS 1 — picos de acesso simultâneo | **Escalabilidade** (e desempenho) |
| RAS 2 — resistir a falhas parciais | **Disponibilidade** |
| RAS 3 — integração com PayPal / Google Maps | **Interoperabilidade** |

## 4. Priorização

**Atributo priorizado: Disponibilidade.**

Justificativa: em um sistema de streaming, uma queda momentânea "só" interrompe o entretenimento — o usuário tenta de novo minutos depois sem prejuízo real. Na FoodNow, no momento em que existe um pedido em andamento (comida sendo preparada, entregador já deslocado), uma indisponibilidade tem consequência física e irreversível: comida estragando, entregador sem instrução de destino, cliente sem rastreio e sem saber se o pedido vai chegar. O custo de uma falha de disponibilidade é maior e mais imediato do que o custo de uma resposta um pouco mais lenta ou de não suportar todo pico teórico de usuários. Por isso, entre escalabilidade, disponibilidade e interoperabilidade, a disponibilidade é o atributo que mais protege a promessa central do produto (entregar o pedido) e foi escolhida como prioridade.

## 5. Decisão Arquitetural

**Decisão:** dividir o backend em subsistemas desacoplados por responsabilidade (Usuários/Autenticação, Pedidos, Entregas), cada um com seu próprio ciclo de falha, e isolar as integrações externas (PayPal, Google Maps) atrás de um padrão de tolerância a falhas (timeout + retry + circuit breaker), em vez de deixar essas chamadas bloqueando o fluxo principal do pedido.

- **Problema que busca resolver:** hoje, se a API do PayPal ou do Google Maps ficar lenta ou indisponível, uma chamada síncrona e sem proteção pode travar threads/conexões do backend inteiro, derrubando também a consulta de restaurantes e o acompanhamento de entregas — funcionalidades que não dependem diretamente daquela integração. Da mesma forma, um pico de carga concentrado no subsistema de Pedidos não deveria conseguir indisponibilizar o subsistema de Entregas.
- **Benefício:** degradação graciosa em vez de queda total — se a integração de pagamento estiver instável, o cliente ainda consegue navegar pelo cardápio e o entregador ainda consegue ver e atualizar entregas já em andamento. O sistema como um todo permanece parcialmente operante mesmo quando uma dependência específica falha.
- **Custo / trade-off:** maior complexidade operacional. Passa a ser necessário monitorar múltiplos subsistemas de forma independente, lidar com consistência eventual entre eles (ex.: um pedido pode estar "pago" no subsistema de Pedidos um instante antes de aparecer disponível para o subsistema de Entregas), implementar e manter a lógica de retry/circuit breaker, e o rastreamento de um único pedido de ponta a ponta (debugging, observabilidade) se torna mais difícil porque a informação está espalhada entre serviços em vez de estar em um único fluxo linear. Há também custo adicional de infraestrutura para rodar e escalar subsistemas de forma independente.

Esse último ponto — mais resiliência em troca de mais complexidade operacional e de depuração — é o principal trade-off desta decisão.
