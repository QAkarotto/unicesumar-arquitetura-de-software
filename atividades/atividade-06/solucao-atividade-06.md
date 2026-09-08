# Atividade 06: Modularidade, Coesão e Acoplamento no FoodNow

**Integrantes:** Lucas Wessendorf de Araujo (RA 23351656-2) e Eduardo Alan dos Santos (RA 23074991-2)

## Problemas identificados

- `Localizacao`: calcula distância "Manhattan" própria e formata coordenada pro provedor de mapa.
- `Endereco`: classifica região/zona e tem fórmula própria de distância e de taxa.
- `Cliente`: calcula distância até o restaurante e tem fórmula própria de taxa.
- `Restaurante`: calcula distância, taxa e tempo próprios.
- `Pedido`: reimplementa distância (diferente do Haversine) e tem taxa própria.
- `DistanciaUtil.calcularKm`: usado por entidades e pelo `FakeMapsClient`, sem dono.
- `TaxaEntregaUtil.calcular`: mais uma fórmula de taxa isolada.
- `PedidoService.criar`: calculava 5 distâncias e 5 taxas diferentes e ficava com a maior.
- `EntregaService.criarPara`: mesmo padrão, com 3 distâncias e 2 tempos.
- `FakeMapsClient.calcularRota`: além de simular o provedor, decide a zona de entrega.

Distância e taxa são calculadas de formas diferentes em vários lugares, sem fonte única de verdade.

## Fluxo de dependências

Antes:

```text
criar pedido -> LocalizacaoService/FakeMapsClient + Restaurante/Endereco/Cliente/Pedido/TaxaEntregaUtil
confirmar -> e-mail
pagar -> FakeMapsClient de novo -> NotificacaoService -> FakeMapsClient de novo
      -> criar entrega -> FakeMapsClient de novo + Restaurante/Pedido -> FakeCourierClient
```

Depois:

```text
criar pedido -> PoliticaEntregaService -> LocalizacaoService/FakeMapsClient
confirmar -> pagar (sem mudança)
      -> criar entrega -> PoliticaEntregaService -> FakeMapsClient -> FakeCourierClient
```

## Decisão e escopo

Criamos `PoliticaEntregaService` e movemos pra lá as contas de distância/área/taxa (pedido) e distância/zona/tempo (entrega) que estavam dentro de `PedidoService` e `EntregaService`. As fórmulas em si não mudaram, só o lugar que as chama — a atividade pede pra preservar os valores atuais, e essas mesmas fórmulas também são usadas pelos endpoints de simulação (`ClienteService`/`RestauranteService`).

Escopo: criação do pedido + criação da entrega, os dois fluxos que mais repetiam a mesma lógica.

## Alterações

- Novo: `PoliticaEntregaService` (`avaliarParaNovoPedido`, `calcularTaxaEntrega`, `avaliarParaDespacho`).
- `PedidoService.criar`: passou a chamar o serviço novo em vez de calcular tudo direto; perdeu a dependência de `LocalizacaoService` e `FakeMapsClient`.
- `EntregaService.criarPara`: mesma ideia; perdeu a dependência de `FakeMapsClient`.

## Testes

Nenhum teste foi alterado. Os testes de API (`FoodNowFlowIT`, `FoodNowErrorsIT`) e os unitários (`PedidoTest`, `LocalizacaoEntregaTest`, `IntegracoesSimuladasTest`) continuaram passando sem mudança, pois cobrem os valores que foram preservados.

## Antes x depois

| | Antes | Depois |
|---|---|---|
| Dependências do `PedidoService` | 8, com `FakeMapsClient` direto | 7, sem `FakeMapsClient` |
| Dependências do `EntregaService` | 5, com `FakeMapsClient` direto | 5, `FakeMapsClient` vira `PoliticaEntregaService` |
| Quem chama `FakeMapsClient` no fluxo pedido→entrega | 3 lugares | 1 lugar |
| Cálculo de distância/taxa | espalhado em `PedidoService`/`EntregaService` | concentrado em `PoliticaEntregaService` |

## Problemas não tratados / trade-offs

- `PagamentoService` e `NotificacaoService` continuam chamando `FakeMapsClient` direto (fora do escopo pedido).
- Fórmulas duplicadas em `Cliente`, `Restaurante`, `Endereco`, `Localizacao`, `Produto`, `ItemPedido` não foram tocadas — usadas pelos testes e pelos endpoints de simulação.
- `FakeMapsClient` continua decidindo zona junto com o cálculo de rota.
- Trade-off: `PoliticaEntregaService` reduz o acoplamento com `FakeMapsClient`, mas mantém a lógica de "pegar o maior valor entre contas divergentes" — só concentrou num lugar só, não eliminou.

## `mvn clean verify`

23 testes unitários + 5 de integração, 0 falhas. Cobertura de linhas (JaCoCo): **97,37%** (mínimo pedido: 80%).
