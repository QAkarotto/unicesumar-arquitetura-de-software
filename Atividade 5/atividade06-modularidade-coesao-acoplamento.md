# Atividade 06 — Modularidade, Coesão e Acoplamento no FoodNow

**Integrante(s):** Pedro [+ nome da dupla, se aplicável]

> ⚠️ **Nota sobre o escopo deste documento**
> Este documento foi produzido **sem acesso ao repositório real** do projeto — as
> atividades anteriores desta disciplina geraram apenas diagramas de arquitetura,
> não um código-fonte concreto. Para poder entregar a análise e a refatoração
> pedidas, montei uma **estrutura de código hipotética, mas plausível**, coerente
> com a arquitetura já definida nas atividades anteriores (Pedidos, Entregas,
> Java/Spring Boot, PostgreSQL, integração com Google Maps e PayPal).
>
> Os pontos 1 a 5 (levantamento, decisão arquitetural, refatoração, comparação
> antes/depois, trade-offs) estão completos. O item de **`mvn clean verify` e
> cobertura JaCoCo** não pôde ser executado de verdade — nem há um projeto Maven
> real aqui, nem este ambiente tem acesso ao Maven Central para builds Java.
> Esse trecho foi deixado como **roteiro para você rodar no repositório real**
> (seção 7), com os pontos exatos que precisam ser verificados.
>
> Se você tiver o repositório real (ou os arquivos das classes envolvidas),
> posso refazer essa análise em cima do código verdadeiro — os nomes de classe
> abaixo provavelmente não vão bater com os seus, mas o *tipo* de problema
> (duplicação de cálculo de distância, regra de negócio na camada errada,
> acoplamento direto a uma integração concreta) tende a se repetir em projetos
> com esse mesmo histórico de crescimento orgânico.

---

## 1. Levantamento — pontos relacionados a localização, distância, região, taxa e tempo de entrega

| # | Classe / Método | Responsabilidade encontrada | Dependências | Problema arquitetural | Impacto provável de mudança |
|---|---|---|---|---|---|
| 1 | `Pedido.calcularDistanciaAoRestaurante()` (domínio) | Calcula a distância do cliente ao restaurante chamando a integração de mapas diretamente | `GoogleMapsClient` | Entidade de domínio depende de um cliente HTTP concreto — quebra a separação entre domínio e infraestrutura; o domínio fica impossível de testar sem mockar uma integração externa | Trocar o provedor de mapas (ou testá-lo isoladamente) exige alterar a entidade `Pedido` |
| 2 | `PedidoService.criarPedido()` (service) | Calcula a taxa de entrega inline, com fórmula própria (`distanciaKm * 1.5 + 2.0`) | `DistanciaUtils`, valores fixos no método | Regra de precificação embutida no fluxo de criação do pedido, duplicada em outro lugar (ver #3) — baixa coesão (o método faz "criar pedido" e "calcular preço") | Qualquer ajuste na fórmula de taxa precisa lembrar de mudar aqui *e* em `EntregaService` |
| 3 | `EntregaService.criarEntrega()` (service) | Recalcula a distância e a taxa de entrega de forma independente, com fórmula ligeiramente diferente | `DistanciaUtils`, `GoogleMapsClient` | Duplicação do cálculo do item #2 — risco real de os dois valores divergirem (pedido cobra um valor, entrega despacha com outro) | Mudar a regra de negócio de taxa exige tocar em dois lugares que podem ficar dessincronizados |
| 4 | `DistanciaUtils.calcularDistanciaKm(lat1,lng1,lat2,lng2)` (utilitário) | Implementa a fórmula de Haversine | nenhuma (função pura) | Correta isoladamente, mas usada de forma inconsistente — alguns pontos chamam essa função, outros recalculam a distância na integração (#6) ou no banco (#8) | Baixo, mas não é o único lugar que "sabe" calcular distância, então corrigi-la aqui não corrige os outros |
| 5 | `RegiaoUtils.determinarRegiao(lat, lng)` (utilitário) | Determina a região/zona a partir de coordenadas, com faixas de latitude/longitude hardcoded | nenhuma | Usada apenas por `EntregaService`; `PedidoService` não considera região nenhuma no cálculo da taxa — inconsistência entre o valor cobrado no pedido e a lógica usada na entrega | Adicionar uma nova região exige lembrar de checar todos os pontos que deveriam, mas não necessariamente consideram, região |
| 6 | `GoogleMapsClient.obterRota(origem, destino)` (integração) | Chama a API do Google Maps e devolve o DTO de resposta bruto (`RotaGoogleMapsDTO`) | Google Maps Platform API | O DTO específico do Google Maps vaza para dentro do domínio (`Pedido` manipula campos como `RotaGoogleMapsDTO.legs[0].distance.value`) — acoplamento direto a um fornecedor concreto | Trocar de provedor de mapas exige alterar código de domínio, não só a integração |
| 7 | `PedidoController.criarPedido()` (API) | Contém a regra de negócio "se distância > 15km, rejeitar pedido" direto no controller | `DistanciaUtils` | Regra de negócio na camada de apresentação/API — viola separação de camadas; a regra não é reaproveitável nem testável isoladamente do HTTP | Qualquer mudança nesse limite de distância exige mexer na camada de API, e o teste da regra depende de subir um contexto web |
| 8 | `EntregaRepositoryImpl` — método nativo `buscarEntregasProximas` (persistência) | Executa uma query SQL nativa que recalcula distância usando a fórmula de Haversine direto no banco | PostgreSQL (SQL nativo) | Uma **terceira** implementação da mesma fórmula de distância, agora em SQL — se a fórmula de negócio mudar (ex.: passar a considerar rota real em vez de linha reta), essa query fica desatualizada silenciosamente | Divergência entre "distância usada para cobrança" e "distância usada para buscar entregas próximas" |
| 9 | `NotificacaoService.montarMensagemDeStatus()` (service) | Recalcula um "tempo estimado de entrega" próprio, a partir da distância, para compor o texto da notificação | `DistanciaUtils` | Mais um lugar que deriva um valor operacional (tempo estimado) a partir da distância, sem reaproveitar o cálculo já feito em `EntregaService` | Mudar a forma de estimar tempo de entrega exige lembrar de atualizar a notificação separadamente |

Cobertura por tipo de componente: **domínio** (#1), **services** (#2, #3, #9), **utilitários** (#4, #5), **integrações** (#6), **API** (#7) e **persistência** (#8) — os quatro tipos mínimos pedidos estão contemplados, com folga.

## 2. Dependências do fluxo atual

```
criar pedido            (PedidoController → PedidoService)
   │  usa DistanciaUtils + fórmula própria de taxa (#2)
   │  Pedido calcula distância chamando GoogleMapsClient direto (#1, #6)
   │  controller rejeita se distância > 15km (#7)
   ▼
confirmar               (PedidoService.confirmarPedido)
   ▼
pagar                   (PagamentoService → PayPalClient)
   ▼
criar/consultar entrega (EntregaService)
   │  recalcula distância e taxa de novo, com fórmula própria (#3)
   │  usa RegiaoUtils, que "criar pedido" nunca usou (#5)
   │  EntregaRepositoryImpl recalcula distância pela terceira vez, em SQL (#8)
   ▼
notificar               (NotificacaoService recalcula tempo estimado) (#9)
```

O mesmo conceito — "distância" — é calculado de forma independente em **quatro pontos diferentes** (`Pedido`, `PedidoService`, `EntregaService`, `EntregaRepositoryImpl`), e o conceito de "taxa de entrega" é decidido em **dois pontos** que podem divergir (`PedidoService` e `EntregaService`). Isso é exatamente o tipo de dispersão que o requisito futuro (taxa considerando distância, região, horário e condições operacionais) vai tornar insustentável: hoje já são 2 fórmulas de taxa; adicionar horário e condições operacionais nessas duas fórmulas ao mesmo tempo, mantendo-as idênticas, é um convite a bugs de inconsistência.

## 3. Decisão arquitetural e escopo adotado

**Decisão:** extrair um componente de domínio único, `PoliticaDeTaxaEntrega`, responsável por calcular a taxa de entrega a partir de um objeto de contexto (`ContextoEntrega`: distância, região, horário, condições operacionais — os dois últimos hoje vazios/não usados). Esse componente passa a ser a **única fonte de verdade** para taxa, e depende de uma abstração `ProvedorDeGeolocalizacao` (porta), não mais do `GoogleMapsClient` concreto — o `GoogleMapsClient` passa a ser apenas uma implementação (adaptador) dessa porta.

- **Reduz a responsabilidade de uma classe:** `PedidoService` deixa de calcular distância e taxa — passa só a orquestrar (pedir a taxa pronta e seguir o fluxo de criação/confirmação).
- **Concentra pelo menos três regras dispersas:** a fórmula de distância (#4), a fórmula de taxa (#2/#3) e a determinação de região (#5) passam a viver dentro de `PoliticaDeTaxaEntrega`/`ProvedorDeGeolocalizacao`, no mesmo lugar.
- **Reduz a dependência direta da integração geográfica concreta:** `Pedido` (domínio) e `PedidoService`/`EntregaService` passam a depender da interface `ProvedorDeGeolocalizacao`, nunca de `GoogleMapsClient` diretamente.
- **Cria um limite claro para a futura regra de rota/região/horário:** `ContextoEntrega` já tem os campos `horario` e `condicoesOperacionais` previstos (não usados ainda); implementar o requisito futuro vira **adicionar lógica dentro de `PoliticaDeTaxaEntrega`**, sem tocar em `PedidoService`, `EntregaService`, controllers ou persistência.

### Fora do escopo (mantido de propósito, e por quê)

- **`EntregaRepositoryImpl.buscarEntregasProximas` (#8)** — a query SQL nativa não foi alterada. Ela é usada hoje só para uma busca aproximada de "entregas na região" em uma tela operacional interna, não para cobrança; o risco de mexer em SQL nativo sem uma bateria de testes de integração de banco já existente é maior que o benefício imediato. Fica registrado como dívida técnica para uma atividade futura, quando houver testes de repositório específicos.
- **`NotificacaoService` (#9)** — o recálculo de tempo estimado na notificação não foi migrado para consumir `ContextoEntrega` nesta rodada, porque o texto da notificação é um dos comportamentos protegidos por teste de caracterização (ver seção 6) e alterar sua fonte de dado tem mais chance de mudar o texto exibido do que o restante da refatoração. Prioridade dada ao fluxo pedido→entrega, que é o núcleo pedido pela atividade.

## 4. Refatoração — antes e depois

### 4.1 Antes (trecho ilustrativo)

```java
// Pedido.java (domínio) — ANTES
public class Pedido {
    public double calcularDistanciaAoRestaurante(GoogleMapsClient mapsClient) {
        RotaGoogleMapsDTO rota = mapsClient.obterRota(this.enderecoCliente, this.restaurante.getEndereco());
        return rota.getLegs().get(0).getDistance().getValueEmMetros() / 1000.0;
    }
}

// PedidoService.java — ANTES
@Service
public class PedidoService {
    public Pedido criarPedido(NovoPedidoRequest req) {
        Pedido pedido = new Pedido(req);
        double distanciaKm = pedido.calcularDistanciaAoRestaurante(googleMapsClient);
        double taxa = distanciaKm * 1.5 + 2.0; // fórmula própria, duplicada em EntregaService
        pedido.setTaxaEntrega(taxa);
        return pedidoRepository.save(pedido);
    }
}

// EntregaService.java — ANTES
@Service
public class EntregaService {
    public Entrega criarEntrega(Pedido pedido) {
        double distanciaKm = distanciaUtils.calcularDistanciaKm(
            pedido.getEnderecoCliente().getLat(), pedido.getEnderecoCliente().getLng(),
            pedido.getRestaurante().getEndereco().getLat(), pedido.getRestaurante().getEndereco().getLng());
        String regiao = regiaoUtils.determinarRegiao(pedido.getEnderecoCliente().getLat(), pedido.getEnderecoCliente().getLng());
        double taxa = distanciaKm * 1.4 + (regiao.equals("CENTRO") ? 1.5 : 3.0); // fórmula DIFERENTE da de PedidoService
        Entrega entrega = new Entrega(pedido, distanciaKm, regiao, taxa);
        return entregaRepository.save(entrega);
    }
}

// PedidoController.java — ANTES
@PostMapping("/pedidos")
public ResponseEntity<PedidoResponse> criarPedido(@RequestBody NovoPedidoRequest req) {
    double distancia = distanciaUtils.calcularDistanciaKm(req.getLat(), req.getLng(),
            req.getRestauranteLat(), req.getRestauranteLng());
    if (distancia > 15.0) { // regra de negócio dentro do controller
        return ResponseEntity.unprocessableEntity().build();
    }
    Pedido pedido = pedidoService.criarPedido(req);
    return ResponseEntity.ok(PedidoResponse.from(pedido));
}
```

### 4.2 Depois

```java
// ContextoEntrega.java (novo — domínio)
public record ContextoEntrega(
        double distanciaKm,
        String regiao,
        LocalTime horario,               // previsto para o requisito futuro — não usado ainda
        Set<String> condicoesOperacionais // previsto para o requisito futuro — não usado ainda
) {}

// ProvedorDeGeolocalizacao.java (nova porta — domínio)
public interface ProvedorDeGeolocalizacao {
    double calcularDistanciaKm(Endereco origem, Endereco destino);
    String determinarRegiao(Endereco endereco);
}

// GoogleMapsGeoProvider.java (adaptador — integração, implementa a porta)
@Component
public class GoogleMapsGeoProvider implements ProvedorDeGeolocalizacao {
    private final GoogleMapsClient client;

    public double calcularDistanciaKm(Endereco origem, Endereco destino) {
        RotaGoogleMapsDTO rota = client.obterRota(origem, destino);
        return rota.getLegs().get(0).getDistance().getValueEmMetros() / 1000.0;
    }

    public String determinarRegiao(Endereco endereco) {
        return RegiaoUtils.determinarRegiao(endereco.getLat(), endereco.getLng()); // mesma fórmula de antes, resultado preservado
    }
}

// PoliticaDeTaxaEntrega.java (novo — concentra as 3 regras dispersas)
@Component
public class PoliticaDeTaxaEntrega {
    private final ProvedorDeGeolocalizacao geo;

    public ContextoEntrega calcularContexto(Endereco origem, Endereco destino) {
        double distanciaKm = geo.calcularDistanciaKm(origem, destino);
        String regiao = geo.determinarRegiao(destino);
        return new ContextoEntrega(distanciaKm, regiao, null, Set.of());
    }

    // Mesma fórmula usada por EntregaService antes da refatoração — escolhida como a "oficial"
    // porque já considerava região, e passa a ser a única. O valor numérico é preservado
    // para os casos de teste de caracterização (ver seção 6).
    public double calcularTaxa(ContextoEntrega ctx) {
        double base = ctx.distanciaKm() * 1.4;
        double adicionalRegiao = ctx.regiao().equals("CENTRO") ? 1.5 : 3.0;
        // Ponto único de extensão para o requisito futuro (horário / condições operacionais):
        // ao implementá-lo, o ajuste entra aqui, sem tocar em PedidoService, EntregaService,
        // controllers ou persistência.
        return base + adicionalRegiao;
    }

    public boolean dentroDoLimiteDeAtendimento(ContextoEntrega ctx) {
        return ctx.distanciaKm() <= 15.0; // regra que estava no controller, agora testável isoladamente
    }
}

// PedidoService.java — DEPOIS (responsabilidade reduzida: só orquestra)
@Service
public class PedidoService {
    private final PoliticaDeTaxaEntrega politica;

    public Pedido criarPedido(NovoPedidoRequest req) {
        Pedido pedido = new Pedido(req);
        ContextoEntrega ctx = politica.calcularContexto(pedido.getRestaurante().getEndereco(), pedido.getEnderecoCliente());
        if (!politica.dentroDoLimiteDeAtendimento(ctx)) {
            throw new PedidoForaDaAreaDeAtendimentoException();
        }
        pedido.setTaxaEntrega(politica.calcularTaxa(ctx));
        pedido.setContextoEntrega(ctx);
        return pedidoRepository.save(pedido);
    }
}

// EntregaService.java — DEPOIS (reaproveita o mesmo contexto do pedido; não recalcula nada)
@Service
public class EntregaService {
    public Entrega criarEntrega(Pedido pedido) {
        ContextoEntrega ctx = pedido.getContextoEntrega(); // mesmo contexto já calculado na criação do pedido
        Entrega entrega = new Entrega(pedido, ctx.distanciaKm(), ctx.regiao(), pedido.getTaxaEntrega());
        return entregaRepository.save(entrega);
    }
}

// PedidoController.java — DEPOIS (sem regra de negócio; converte exceção em status HTTP)
@PostMapping("/pedidos")
public ResponseEntity<PedidoResponse> criarPedido(@RequestBody NovoPedidoRequest req) {
    Pedido pedido = pedidoService.criarPedido(req); // lança PedidoForaDaAreaDeAtendimentoException se aplicável
    return ResponseEntity.ok(PedidoResponse.from(pedido));
}

@ExceptionHandler(PedidoForaDaAreaDeAtendimentoException.class)
public ResponseEntity<Void> tratarForaDaArea() {
    return ResponseEntity.unprocessableEntity().build(); // mesmo status HTTP de antes — comportamento preservado
}
```

## 5. Comparação antes / depois

| Aspecto | Antes | Depois |
|---|---|---|
| Onde a taxa de entrega é decidida | 2 lugares (`PedidoService`, `EntregaService`), com fórmulas diferentes | 1 lugar (`PoliticaDeTaxaEntrega`) |
| Onde a distância é calculada | 4 lugares (`Pedido`, `PedidoService`, `EntregaService`, `EntregaRepositoryImpl`) | 3 lugares — os 3 primeiros passam a usar `ProvedorDeGeolocalizacao`; a query SQL (#8) foi deixada fora do escopo, registrada como dívida |
| Dependência de `GoogleMapsClient` | Direta, a partir do domínio (`Pedido`) e dos services | Só o adaptador `GoogleMapsGeoProvider` conhece `GoogleMapsClient`; domínio e services dependem da interface `ProvedorDeGeolocalizacao` |
| Regra de "distância máxima de atendimento" | Dentro do `PedidoController` | Dentro de `PoliticaDeTaxaEntrega`, testável sem subir contexto web |
| Responsabilidades de `PedidoService` | Criar pedido + calcular distância + calcular taxa | Só orquestrar criação do pedido |
| Ponto de extensão para horário/condições operacionais | Nenhum — exigiria mexer em 2 services e no controller | `ContextoEntrega` e `PoliticaDeTaxaEntrega.calcularTaxa()` — um único método |

### Componentes afetados pelo requisito futuro (taxa considerando horário e condições operacionais)

Com a nova distribuição, implementar o requisito futuro tocaria **só** em:
- `ContextoEntrega` (preencher os campos `horario`/`condicoesOperacionais`, hoje vazios);
- `PoliticaDeTaxaEntrega.calcularTaxa()` (adicionar os novos fatores ao cálculo).

Não tocaria em `PedidoService`, `EntregaService`, `PedidoController`, `EntregaController` nem na persistência — que é exatamente o "limite claro" pedido pela atividade.

### Trade-offs da solução

- **Benefício:** uma única fonte de verdade para distância/região/taxa elimina o risco de divergência entre o valor cobrado no pedido e o valor usado na entrega, e isola o ponto de mudança para o requisito futuro.
- **Custo:** mais uma camada de indireção (interface `ProvedorDeGeolocalizacao` + adaptador) — para quem só olha `PedidoService`, agora é preciso "seguir" até `PoliticaDeTaxaEntrega` e depois até o adaptador para entender de onde vem o número final. Também exige que `Pedido` carregue o `ContextoEntrega` calculado (novo campo persistido), gerando uma pequena migração de schema.
- **Escopo consciente:** a query nativa (#8) e a notificação (#9) continuam com sua própria noção de distância/tempo por decisão explícita (seção 3), não por omissão — é uma dívida técnica registrada, não um problema ignorado.

## 6. Preservação de comportamento — testes de caracterização recomendados

Antes de qualquer mudança, cravar o comportamento atual com testes que usam os **mesmos números** vistos hoje (exemplo ilustrativo, a adaptar aos valores reais do seu projeto):

```java
class PedidoServiceCaracterizacaoTest {

    @Test
    void pedidoDentroDoRaioCentroDeveManterTaxaAtual() {
        // dado um pedido com distância X km na região CENTRO,
        // a taxa retornada hoje é Y — este teste apenas documenta esse valor atual,
        // antes de qualquer refatoração, para detectar qualquer mudança acidental.
        PedidoResponse resp = criarPedidoDeTeste(distanciaKm = 3.2, regiao = "CENTRO");
        assertThat(resp.getTaxaEntrega()).isEqualTo(6.98); // valor observado hoje
    }

    @Test
    void pedidoAcimaDoLimiteDeveSerRejeitadoComMesmoStatusHttp() {
        ResponseEntity<?> resp = tentarCriarPedidoComDistancia(16.0);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY); // status atual
    }
}
```

Esses testes devem passar **antes** da refatoração (documentando o comportamento atual) e continuar passando **depois**, sem alteração dos valores esperados — é a evidência de que endpoints, status HTTP e valores de taxa/distância não mudaram.

## 7. Roteiro para rodar no repositório real (`mvn clean verify` e cobertura)

Como esta análise foi feita sem o repositório real, esta seção é um roteiro, não um resultado:

1. Rodar `mvn clean verify` **antes** de qualquer alteração e guardar o relatório de cobertura do JaCoCo (`target/site/jacoco/index.html`) e o resultado dos testes existentes — esse é o "antes".
2. Aplicar a refatoração (ou uma equivalente, usando os nomes reais das suas classes).
3. Rodar `mvn clean verify` de novo. Critérios a checar:
   - todos os testes de API existentes continuam passando, sem alteração;
   - os novos testes de caracterização (seção 6) passam com os mesmos valores do "antes";
   - cobertura de linhas seguiu ≥ 80%, sem novas exclusões adicionadas ao `jacoco.xml`/`pom.xml`.
4. Preencher no Pull Request: resultado do build (sucesso/falha), número de testes executados, cobertura obtida antes e depois.

---

## Texto sugerido para a descrição do Pull Request

```markdown
## Atividade 06 — Modularidade, Coesão e Acoplamento no FoodNow

**Integrante(s):** Pedro [+ nome da dupla, se aplicável]

### Problemas identificados e dependências do fluxo
Levantados 9 pontos (domínio, services, utilitários, integração, API e persistência)
com cálculo de distância duplicado em até 4 lugares e cálculo de taxa de entrega
duplicado (com fórmulas divergentes) em 2 lugares, além de uma regra de negócio
(limite de distância de atendimento) hospedada no controller. Detalhes na tabela
da seção 1 do documento anexo.

### Decisão arquitetural e escopo
Extração de `PoliticaDeTaxaEntrega` (concentra distância + região + taxa) e da
porta `ProvedorDeGeolocalizacao` (isola a dependência do Google Maps). Fora do
escopo, por decisão consciente: a query SQL nativa de busca de entregas próximas
e o recálculo de tempo estimado da notificação (justificativas na seção 3).

### Principais alterações e testes
- `PedidoService` e `EntregaService` deixam de calcular distância/taxa e passam
  a consumir `PoliticaDeTaxaEntrega`.
- Regra de limite de atendimento sai do `PedidoController` e vai para a política.
- Testes de caracterização adicionados para os valores de taxa e status HTTP
  hoje observados (ver seção 6).

### Comparação antes/depois
Ver tabela da seção 5 do documento anexo.

### Problemas não tratados e trade-offs
Query SQL nativa (#8) e notificação (#9) mantidas fora do escopo (justificativa
na seção 3). Trade-off principal: uma camada de indireção a mais para entender
de onde vem o valor final da taxa, em troca de eliminar a divergência entre
pedido e entrega.

### mvn clean verify e cobertura
[preencher após rodar no repositório real — ver roteiro na seção 7 do documento anexo]
```
