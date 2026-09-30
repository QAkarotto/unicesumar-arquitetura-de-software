# Atividade 06 — Modularidade, coesão e acoplamento (localização e entrega)

## 1. Integrantes

- Edi Carlos Francisco Junior
- Danilo Bolzan

---

## 2. Problemas identificados

Levantamento feito sobre o código **antes** da refatoração (12 pontos; cobre domínio, services, utilitários, integrações, API e persistência).

### 2.1 Pontos do código

**1. `PedidoService.criar` — service**
- Responsabilidade: valida cliente/restaurante/endereço, escolhe a distância entre **5 medidas** (rota do provedor, haversine, plana por endereço, Manhattan), decide o atendimento (3 condições), calcula a taxa como a **maior de 5 fórmulas** e soma o adicional geográfico dos itens.
- Dependências: 4 repositories, `LocalizacaoService`, `FakeMapsClient` (direto), `Restaurante`, `Cliente`, `Endereco`, `Pedido`, `TaxaEntregaUtil`.
- Problema: método que mistura orquestração com regra de negócio; consulta a rota duas vezes (via `LocalizacaoService` e direto no `FakeMapsClient`); depende de uma integração concreta.
- Impacto: qualquer nova regra de taxa/distância obriga a editar este método e decidir o que fazer com cada uma das 5 fórmulas.

**2. `PedidoService.confirmar` — service**
- Responsabilidade: confirma o pedido e monta o texto do e-mail com região e estimativa de tempo (`Pedido.determinarRegiaoEntrega`, `Pedido.estimarTempoEntregaPeloPedido`).
- Dependências: `FakeEmailClient`, `Pedido`.
- Problema: o service de pedido conhece o texto da notificação e chama regras de região/tempo.
- Impacto: mudar região ou tempo altera a mensagem enviada e este service.

**3. `Pedido` (7 métodos geográficos) — domínio**
- Métodos: `calcularDistanciaEntrega` (plana, 111 km/grau), `calcularTaxaEntregaPorDistancia` (5,00 + 1,25·d), `estimarTempoEntregaPeloPedido` (14 + 3,6·d + 2·itens), `determinarRegiaoEntrega`, `verificarEnderecoAtendido`, `possuiDivergenciaDeDistancia`, `calcularAdicionalGeograficoDosItens`.
- Dependências: `Restaurante`, `Endereco`, `ItemPedido`.
- Problema: a entidade do pedido calcula geografia; terceira fórmula de distância; baixa coesão (dinheiro + status + geografia). Três desses métodos não eram usados em produção.
- Impacto: alterar distância/taxa/tempo altera uma entidade persistida.

**4. `Restaurante.calcularDistanciaAte`, `calcularTaxaEntrega`, `atendeEndereco`, `classificarRegiaoDeEntrega`, `estimarTempoEntrega` — domínio**
- Responsabilidade: distância haversine, taxa (4,00 + 1,35·d), atendimento, região PROXIMA/LIMITE, tempo (12 + 3,8·d).
- Dependências: `DistanciaUtil`, `Endereco`.
- Problema: segunda fórmula de taxa e de tempo; regra de atendimento na entidade.
- Impacto: quem altera a taxa precisa lembrar desta cópia.

**5. `Endereco.calcularDistanciaAte`, `estimarMinutosAte`, `classificarZonaDeEntrega`, `pertenceARegiaoDo`, `calcularTaxaLocalAte` — domínio**
- Responsabilidade: distância plana (110,57/96,48), tempo (8 + 4·d), zona (CENTRAL/URBANA/EXTERNA), mesma região (cidade + prefixo do CEP), taxa local (3,75 + 1,45·d + 3,50 fora da região).
- Problema: uma entidade de valor com quatro regras geográficas; zona e região definidas por texto (bairro/cidade/CEP).
- Impacto: a futura regra de região precisa mexer aqui e em todos que chamam `classificarZonaDeEntrega`.

**6. `Cliente.calcularDistanciaAte`, `estaDentroDaAreaDeEntrega`, `calcularTaxaEntregaDoRestaurante`, `estimarTempoAte`, `identificarRegiaoPrincipal` — domínio**
- Responsabilidade: distância/taxa (3,90 + 1,10·d)/tempo do cliente até o restaurante, sempre pelo **endereço principal** (não pelo endereço de entrega do pedido).
- Problema: quarta fórmula de taxa; decisão de atendimento baseada em outro endereço que o do pedido (comportamento que precisa ser preservado).
- Impacto: mudar “o endereço considerado” exige mexer em `Cliente` e em `PedidoService.criar`.

**7. `Produto.calcularAdicionalRegional`, `Produto.podeSerEntregueEm`, `ItemPedido.calcularParcelaGeografica` — domínio**
- Responsabilidade: adicional de 0,08/km por item fora da zona CENTRAL; produto decide se atende o endereço.
- Dependências: `Restaurante`, `Endereco` (zona e distância).
- Problema: produto e item de pedido conhecem zona e distância.
- Impacto: o adicional por item fica invisível para quem estuda a taxa de entrega.

**8. `TaxaEntregaUtil.calcular` e `DistanciaUtil.calcularKm` — utilitários**
- Responsabilidade: taxa por faixa (≤ 3 km: 4,50; senão 6,00; + 1,20/km) e distância haversine.
- Dependências: chamados estaticamente por `PedidoService`, `Restaurante`, `Cliente`, `Entrega`, `FakeMapsClient`.
- Problema: utilitários estáticos sem dono; a “quinta” fórmula de taxa é um utilitário.
- Impacto: não há um ponto único para trocar cálculo; testes não conseguem substituí-los.

**9. `FakeMapsClient.calcularRota` / `buscarCoordenadas` — integração**
- Responsabilidade: distância do trajeto (linha reta × 1,15), duração (12 + 3,5·d) e zona (CENTRAL ≤ 5 km, senão EXPANDIDA).
- Dependentes diretos: **7 classes** (`PedidoService`, `EntregaService`, `PagamentoService`, `NotificacaoService`, `LocalizacaoService`, `ClienteService`, `RestauranteService`).
- Problema: os records do provedor (`RouteResult`, `MapCoordinates`, strings) vazam para toda a camada de serviço; a zona de negócio é decidida pelo provedor.
- Impacto: trocar o provedor ou consultar “rota real” altera 7 classes.

**10. `EntregaService.criarPara` — service**
- Responsabilidade: escolhe distância (máximo de 3 medidas), zona (a rota “EXPANDIDA” sobrepõe a classificação por endereço), tempo (máximo de 2 estimativas + tempo de coleta) e monta o payload do entregador.
- Dependências: `FakeMapsClient`, `FakeEmailClient`, `FakeCourierClient`, 2 repositories, `Pedido`, `Endereco`.
- Problema: regra de despacho misturada com chamada a três integrações concretas.
- Impacto: regra de horário/condição operacional afetaria distância, zona e tempo neste método.

**11. `PagamentoService.processar` + `Pagamento.possuiRiscoGeografico` — service / persistência**
- Responsabilidade: consulta a rota apenas para gravar `distanciaValidadaKm`, calcula a região enviada ao gateway e persiste `regiaoEntrega`; o risco geográfico (> 15 km, EXTERNA ou NAO_ATENDIDA) é regra da entidade.
- Dependências: `FakeMapsClient`, `FakePaymentGateway`, `NotificacaoService`, `EntregaService`.
- Problema: pagamento depende de mapas; campos geográficos persistidos no pagamento.
- Impacto: mudar região/distância muda o que é gravado e o cálculo de risco.

**12. `NotificacaoService.notificarPagamento` + `Notificacao.adicionarReferenciaGeografica` — service / domínio**
- Responsabilidade: consulta a rota só para redigir a mensagem (duração ou zona) e anexa “Região de entrega: zona [coordenadas]”.
- Dependências: `FakeEmailClient`, `FakeMapsClient`, `Endereco`.
- Problema: formatação de mensagem misturada com regra geográfica; consulta de mapas por causa de um texto.
- Impacto: mudar a zona altera mensagens enviadas.

**Outros pontos observados (mesmo problema, não listados na tabela principal):** `ClienteService.adicionarEndereco` (dois geocodings redundantes, com ramo `APPROXIMATE` que reatribui os mesmos valores); `ClienteService.simularAtendimento` e `RestauranteService.simularEntrega` (recalculam distância/taxa/tempo/região com fórmulas próprias e expõem valores diferentes na API); `ApiDtos.EntregaResponse` (`custoOperacional` calculado em `Entrega.calcularCustoOperacional` a cada GET); `LocalizacaoService.calcularDistancia/estimarTempoEntrega` (wrapper fino; o segundo nunca era usado).

### 2.2 Dependências do fluxo

**Antes**

```text
criar pedido -> confirmar -> pagar -> criar/consultar entrega

POST /pedidos
  PedidoService.criar
    -> ClienteRepository, RestauranteRepository, ProdutoRepository, PedidoRepository
    -> LocalizacaoService -> FakeMapsClient
    -> FakeMapsClient                                   (2ª consulta de rota)
    -> Restaurante / Endereco / Cliente / Pedido / Localizacao   (5 medidas de distância)
    -> TaxaEntregaUtil + 4 fórmulas de taxa nas entidades
    -> Produto.calcularAdicionalRegional / ItemPedido.calcularParcelaGeografica

POST /pedidos/{id}/confirmar
  PedidoService.confirmar
    -> Pedido.confirmar
    -> Pedido.determinarRegiaoEntrega / estimarTempoEntregaPeloPedido
    -> FakeEmailClient

POST /pagamentos
  PedidoService.pagar -> PagamentoService.processar
    -> FakeMapsClient (rota)  +  Pedido.determinarRegiaoEntrega
    -> FakePaymentGateway
    -> Pedido.registrarPagamento, PagamentoRepository
    -> NotificacaoService -> FakeMapsClient (rota) + FakeEmailClient
    -> [se APROVADO] EntregaService.criarPara
         -> FakeMapsClient (rota) + Pedido/Endereco (distância, zona, tempo)
         -> FakeCourierClient
         -> EntregaRepository

GET /entregas/pedido/{id}
  EntregaService.consultarPorPedido -> EntregaRepository
  ApiDtos.EntregaResponse -> Entrega.calcularCustoOperacional

PATCH /entregas/{id}/status (EM_ROTA)
  EntregaService.atualizarStatus -> Pedido.iniciarEntrega -> FakeEmailClient
```

Consultas de rota ao provedor em um fluxo completo: **5** (2 na criação, 1 no pagamento, 1 na notificação, 1 na entrega).

**Depois**

```text
POST /pedidos
  PedidoService.criar
    -> repositories
    -> PoliticaEntrega.cotar(SolicitacaoCotacao)  -> ProvedorGeografico (porta)
    -> PoliticaEntrega.taxaFinal(cotacao, pedido)

POST /pedidos/{id}/confirmar
  PedidoService.confirmar -> Pedido.confirmar -> NotificacaoService.notificarConfirmacao
                                                   -> PoliticaEntrega (região, tempo) + FakeEmailClient

POST /pagamentos
  PagamentoService.processar
    -> PoliticaEntrega.avaliar(pedido)   -> ProvedorGeografico (porta)   (rota + região)
    -> FakePaymentGateway, repositories
    -> NotificacaoService.notificarPagamento(pedido, status, rota)      (reaproveita a rota)
    -> [se APROVADO] EntregaService.criarPara
         -> PoliticaEntrega.planejarEntrega(pedido) -> ProvedorGeografico (porta)
         -> FakeCourierClient, EntregaRepository

PATCH /entregas/{id}/status (EM_ROTA)
  EntregaService.atualizarStatus -> NotificacaoService.notificarEntregaIniciada

ProvedorGeografico  <-- implementa --  MapsProvedorGeografico -> FakeMapsClient   (único ponto)
```

Consultas de rota em um fluxo completo: **3**.

---

## 3. Decisão arquitetural e escopo adotado

**Decisão.** Introduzir dois limites:

1. **Porta `ProvedorGeografico`** (`geo`), com tipos próprios do domínio (`Rota`, `CoordenadasResolvidas`), e o **adapter `MapsProvedorGeografico`** (`integration`), que é o único que conhece o `FakeMapsClient`.
2. **`PoliticaEntrega`** (`entrega`), interface que concentra as decisões de distância efetiva, atendimento, taxa, região, zona de despacho e tempo. `PoliticaEntregaPadrao` reproduz exatamente as regras atuais e depende apenas da porta.

Os services passam a **orquestrar**: buscam dados, chamam a política, persistem, notificam. O envio de e-mails do fluxo passou para `NotificacaoService` (`notificarConfirmacao`, `notificarPagamento`, `notificarEntregaIniciada`).

**Limite para a futura regra (rota, região, horário).** A nova regra deve ser uma implementação/evolução de `PoliticaEntrega`. Novos insumos (horário, condições operacionais) entram no record `SolicitacaoCotacao` sem alterar a assinatura de quem chama. Os services não precisam mudar. **A regra de horário não foi implementada.**

**Escopo.**
- Dentro: criação do pedido, confirmação, pagamento, criação da entrega, notificações desses passos e o isolamento do provedor geográfico em todos os services.
- Fora (registrado na seção 7): fórmulas de taxa/tempo que ainda servem às simulações, `Entrega.calcularCustoOperacional`, `Pagamento.possuiRiscoGeografico`, `Notificacao.adicionarReferenciaGeografica`, integrações de e-mail/entregador/pagamento.

**Comportamento preservado (inclusive as particularidades).** Continuam existindo resultados distintos em campos distintos: `distanciaEntregaKm` do pedido (máximo de 5 medidas), `distanciaValidadaKm` do pagamento (rota do provedor) e `distanciaKm` da entrega (máximo de 3 medidas); a decisão de atendimento continua usando o endereço principal do cliente; a taxa continua sendo a maior das 5 fórmulas mais o adicional dos itens.

---

## 4. Principais alterações

**Novos**
- `geo/ProvedorGeografico`, `geo/Rota`, `geo/CoordenadasResolvidas`
- `integration/MapsProvedorGeografico` (adapter)
- `entrega/PoliticaEntrega`, `PoliticaEntregaPadrao`, `SolicitacaoCotacao`, `CotacaoEntrega`, `AvaliacaoGeografica`, `PlanoEntrega`

**Alterados**
- `PedidoService`: sem `FakeEmailClient`, `FakeMapsClient`, `LocalizacaoService`; sem `maiorTaxa`, sem cálculo de distância/taxa/atendimento.
- `PagamentoService`, `EntregaService`, `NotificacaoService`: sem `FakeMapsClient`; usam `PoliticaEntrega` e a rota recebida. `EntregaService` deixou de depender de `FakeEmailClient`.
- `ClienteService`, `RestauranteService`, `LocalizacaoService`: passam a depender da porta. `ClienteService.adicionarEndereco` deixou de geocodificar duas vezes (mesmo resultado).
- `Pedido`: removidos 7 métodos geográficos. `Produto.calcularAdicionalRegional` e `ItemPedido.calcularParcelaGeografica`: removidos (regra agora em `PoliticaEntregaPadrao`).
- `LocalizacaoService.calcularDistancia` e `estimarTempoEntrega`: removidos.

**Não alterados:** controllers, DTOs, endpoints, JSON, `FakeMapsClient` e demais integrações, `pom.xml`/JaCoCo (sem novas exclusões), testes de API existentes.

---

## 5. Testes realizados

- **Testes de API existentes** (`FoodNowFlowIT`, `FoodNowErrorsIT`): sem modificação.
- **Novo — `CaracterizacaoEntregaIT`**: fixa valores **exatos** (taxa, total, distância do pedido, do pagamento e da entrega, tempo, região, zona, custo operacional e texto de cada e-mail) em 3 cenários (zona central; fora do centro com zona expandida; pagamento rejeitado) e nas simulações de atendimento e de entrega. Os valores esperados vêm da execução da versão original.
- **Novo — `PoliticaEntregaPadraoTest`**: testa a política **sem Spring e sem `FakeMapsClient`**, com um provedor fixo (evidencia a redução de acoplamento).
- **Novo — `MapsProvedorGeograficoTest`**: tradução do adapter (coordenadas exatas/aproximadas e rota).
- **Ajustado — `PedidoTest`**: removidas as asserções dos métodos geográficos que saíram de `Pedido` (agora cobertas por `PoliticaEntregaPadraoTest`).
- **Comparação lado a lado (versão anterior × nova).** Para 20 000 cenários gerados (posições, raios, bairros, cidades, CEPs), a aritmética de criação, confirmação, pagamento, notificação e entrega foi executada nas duas versões, e para 3 000 cenários os services reais foram executados com repositórios em memória (criar, adicionar item, confirmar, pagar aprovado/rejeitado, consultar e atualizar entrega): as saídas (valores, status, exceções e e-mails) foram idênticas byte a byte.

---

## 6. Comparação antes/depois

| Aspecto | Antes | Depois |
|---|---|---|
| Classes que dependem diretamente de `FakeMapsClient` | 7 | 1 (`MapsProvedorGeografico`) |
| Integrações concretas injetadas em `PedidoService` | 2 (`FakeEmailClient`, `FakeMapsClient`) | 0 |
| Integrações concretas em `EntregaService` | 3 | 1 (`FakeCourierClient`) |
| Integrações concretas em `PagamentoService` / `NotificacaoService` | 2 / 2 | 1 / 1 |
| Linhas de `PedidoService` | 148 | 120 |
| Métodos públicos em `Pedido` | 24 | 17 |
| Consultas de rota em um fluxo completo | 5 | 3 |
| Onde está a escolha “maior distância / maior taxa / maior tempo” do fluxo | `PedidoService`, `EntregaService` | `PoliticaEntregaPadrao` |
| Onde `Pedido`/`Produto`/`ItemPedido` calculavam geografia | 9 métodos | 0 |

**Regras concentradas em `PoliticaEntregaPadrao`** (antes espalhadas): (1) distância efetiva de criação (máximo de 5 medidas); (2) elegibilidade de atendimento (3 condições); (3) taxa base (maior de 5 fórmulas) e taxa final (+ adicional dos itens); (4) região de entrega; (5) zona e distância de despacho; (6) tempo estimado (confirmação e entrega).

**Componentes afetados pelo requisito futuro**
- Antes: `PedidoService.criar`, `Pedido` (7 métodos), `Produto`, `ItemPedido`, `Restaurante`, `Endereco`, `Cliente`, `TaxaEntregaUtil`, `EntregaService`, `PagamentoService`, `NotificacaoService`, `FakeMapsClient` e as duas simulações — cerca de 15 componentes.
- Depois: `PoliticaEntregaPadrao` (e, se entrarem novos insumos, `SolicitacaoCotacao`/`CotacaoEntrega`) e o adapter `MapsProvedorGeografico` se a rota mudar. Ainda dependem das fórmulas legadas: `Restaurante.calcularTaxaEntrega`, `Endereco.calcularTaxaLocalAte`, `Cliente.calcularTaxaEntregaDoRestaurante` e `TaxaEntregaUtil` (ver seção 7).

**Dependências reduzidas:** services → `FakeMapsClient` (7 → 0); `PedidoService` → `FakeEmailClient`; `EntregaService` → `FakeEmailClient`; `PedidoService` → `LocalizacaoService`; entidades de pedido → regras geográficas.

---

## 7. Problemas não tratados e trade-offs

**Não tratados (com justificativa)**
- **Fórmulas de taxa em `Restaurante`, `Endereco`, `Cliente` e `TaxaEntregaUtil`**: continuam nas entidades/utilitário porque `simular-entrega` e `atendimento` expõem esses valores na API; movê-las exigiria reescrever as simulações. A política ainda as chama. Próximo passo natural.
- **`ClienteService.simularAtendimento` e `RestauranteService.simularEntrega`**: só trocaram o provedor pela porta; continuam calculando com as entidades.
- **`Entrega.calcularCustoOperacional` e `Pagamento.possuiRiscoGeografico`**: regras de negócio expostas por DTO; alterá-las não era necessário para o objetivo e aumentaria o risco de mudar a API.
- **`Notificacao.adicionarReferenciaGeografica`**: ainda mistura formatação com `Endereco.classificarZonaDeEntrega`; tem teste próprio (`IntegracoesSimuladasTest`) que se manteve.
- **Código de produção só usado em testes** (`Entrega.calcularDistancia`, `estimarTempoEntrega`, `calcularDistanciaPeloEndereco`, construtor de 4 argumentos): mantido para não alterar testes existentes fora do foco.
- **Strings de região/zona** (`CENTRAL`, `URBANA`, `EXTERNA`, `NAO_ATENDIDA`…) sem enum: trocá-las mudaria o JSON.
- **`FakeEmailClient`, `FakeCourierClient` e `FakePaymentGateway`** continuam concretos nos services: não são integrações geográficas.
- **Múltiplas medidas de distância**: preservadas por exigência de comportamento; a política as explicita, mas elas ainda existem.

**Trade-offs**
- Mais tipos e indireção (3 em `geo`, 6 em `entrega`, 1 adapter) para ganhar um ponto único de mudança e testes sem Spring.
- `Rota.zona` ainda carrega o vocabulário do provedor (`CENTRAL`/`EXPANDIDA`), porque a API expõe essa zona.
- `PoliticaEntrega` reúne cotação, avaliação e plano de despacho (6 métodos); quando a regra de horário chegar, pode valer dividir a interface.
- A política depende das entidades do domínio (`Cliente`, `Restaurante`, `Pedido`); é uma dependência de direção correta (serviço → domínio), mas ainda não isola o domínio das fórmulas antigas.
- Os e-mails do fluxo passam a sair de `NotificacaoService` (mais coesão), que agora depende da política.

---