# Notas do professor — FoodNow

Este documento descreve o gabarito arquitetural da versão inicial do FoodNow. O sistema está funcional e coberto por testes, mas foi estruturado como um monólito organizado por camadas técnicas e contém problemas arquiteturais moderados e intencionais. O objetivo é analisar distribuição de responsabilidades e custo de mudança, não procurar defeitos de estilo ou de funcionamento.

## Visão geral da arquitetura inicial

O código principal está dividido em `controller`, `service`, `repository`, `model`, `integration` e `util`. Essa separação dá aparência de arquitetura em camadas, porém cada funcionalidade atravessa vários pacotes e os limites conceituais entre cliente, restaurante, pedido, pagamento, entrega, localização e notificação não estão claros.

O fluxo principal acopla diretamente regras de domínio, persistência e integrações simuladas:

```text
PagamentoController
  -> PedidoService
     -> repositórios de Cliente, Restaurante, Produto e Pedido
     -> LocalizacaoService -> FakeMapsClient
     -> TaxaEntregaUtil
     -> FakeEmailClient
     -> PagamentoService
        -> FakePaymentGateway
        -> PedidoRepository e PagamentoRepository
        -> NotificacaoService -> FakeEmailClient
        -> EntregaService -> FakeMapsClient e FakeEmailClient
```

As implementações `Fake*` são adequadas para manter a atividade offline, mas seu uso direto é propositalmente parte do problema.

# Problemas relacionados a Localização

Localização não possui um limite arquitetural único. Coordenadas, geocodificação, linha reta, rota, área de atendimento, taxa e estimativa são decididas ou conhecidas em entidades, serviços, utilitários e no cliente simulado de mapas.

| Responsabilidade | Classe atual | Por que está mal posicionada | Impacto |
|---|---|---|---|
| Calcular distância do endereço principal ao restaurante | `model.Cliente` | Cliente passa a conhecer geometria e localização do restaurante | Alterar algoritmo geográfico modifica uma entidade de cliente |
| Decidir se o cliente está na área de entrega | `model.Cliente` | Mistura cadastro do cliente com política logística | Nova política por região/horário afeta cliente |
| Calcular distância até o destino | `model.Restaurante` | Restaurante executa cálculo que também aparece em outras classes | Regra duplicada e resultado possivelmente divergente |
| Calcular taxa de entrega | `model.Restaurante` | Política de preço logístico está dentro do cadastro do restaurante | Mudanças comerciais/logísticas alteram a entidade |
| Expor latitude e longitude | `model.Restaurante` | A entidade publica detalhes usados por cálculos externos | Formato de localização vaza pelo domínio |
| Calcular distância linear própria | `model.Pedido` | Pedido reimplementa aproximação em graus, diferente do Haversine e da rota | Três algoritmos podem produzir distâncias distintas |
| Calcular taxa própria | `model.Pedido` | Mantém outra fórmula além de `Restaurante` e `TaxaEntregaUtil` | Uma mudança de tarifa exige vários ajustes |
| Verificar endereço atendido | `model.Pedido` | Duplica a comparação de raio existente em cliente e serviço | Critério de atendimento fica inconsistente |
| Calcular distância da entrega | `model.Entrega` | A entrega repete cálculo por Haversine | Mudança para distância de trajeto alcança outra entidade |
| Estimar tempo | `model.Entrega` | Possui fórmula própria, além do tempo retornado por mapas | Estimativas podem divergir |
| Fórmula Haversine | `util.DistanciaUtil` | É usada por entidades e pelo fake, tornando-se dependência compartilhada sem dono funcional | Alteração propaga-se por domínio e integração |
| Faixas e fórmula de taxa | `util.TaxaEntregaUtil` | Parte das regras está em utilitário estático, parte em entidades | Não existe fonte única de verdade |
| Converter resposta do provedor em coordenadas | `service.LocalizacaoService` | O serviço conhece diretamente `FakeMapsClient.MapCoordinates` e strings do provedor | Troca de provedor exige alterar serviço |
| Geocodificar endereço de cliente | `service.ClienteService` | Serviço de cliente reconstrói o endereço após consultar mapas | Cadastro de cliente muda quando muda a geocodificação |
| Geocodificar endereço de restaurante | `service.RestauranteService` | Repete fluxo semelhante ao de cliente | Mudança na resposta de mapas atinge mais de uma funcionalidade |
| Consultar rota e validar raio | `service.PedidoService` | Coordena integração geográfica e aplica política logística | Serviço de pedidos concentra outra razão para mudar |
| Consumir rota concreta e tempo | `service.EntregaService` | Depende diretamente de `FakeMapsClient.RouteResult` | Detalhes do provedor vazam na entrega |
| Determinar distância de trajeto, duração e zona | `integration.FakeMapsClient` | Além de simular o fornecedor, fixa regras que também influenciam negócio | Limite entre fornecedor e regra interna fica ambíguo |

Há duplicação plausível, não idêntica: `Cliente`, `Restaurante` e `Entrega` usam Haversine por `DistanciaUtil`; `Pedido` usa aproximação cartesiana; `FakeMapsClient` multiplica a linha reta por 1,15 para simular rota. As taxas também diferem entre `Restaurante`, `Pedido` e `TaxaEntregaUtil`. Isso representa crescimento orgânico e permite discutir inconsistência sem tornar o código ilegível.

# Cenário de mudança: nova regra de localização

Requisito proposto: o cálculo deve considerar distância do trajeto, região de entrega e horário, em vez de apenas distância linear.

Antes da refatoração, a análise alcança pelo menos:

- `Cliente`, em `calcularDistanciaAte` e `estaDentroDaAreaDeEntrega`;
- `Restaurante`, nos cálculos de distância/taxa e na exposição de coordenadas;
- `Pedido`, no cálculo próprio de distância, taxa e atendimento;
- `Entrega`, no cálculo e estimativa próprios;
- `ClienteService` e `RestauranteService`, por conhecerem o fluxo de geocodificação;
- `PedidoService`, por buscar rota, validar raio e calcular taxa;
- `EntregaService`, por consumir diretamente resposta concreta de rota;
- `LocalizacaoService`, por converter a estrutura concreta do fake;
- `DistanciaUtil` e `TaxaEntregaUtil`, pelas fórmulas estáticas;
- `FakeMapsClient`, para passar região, horário e uma rota mais rica;
- DTOs e testes de API se região/horário passarem a fazer parte do contrato observável.

Possíveis caminhos, sem determinar uma resposta única:

- agrupar capacidades em um módulo/pacote de localização e entrega;
- definir um serviço de aplicação que produza um resultado de rota interno com distância, região e duração;
- encapsular o cliente de mapas atrás de uma porta ou adapter;
- criar uma política ou calculadora de taxa que receba os dados relevantes;
- manter entidades com dados e invariantes, deslocando consultas externas para serviços;
- usar objetos de valor para rota, região e janela de horário;
- consolidar regras duplicadas gradualmente, preservando os endpoints e os testes.

Uma solução pode continuar em camadas, migrar para pacotes por funcionalidade ou combinar módulos funcionais com camadas internas. O ponto de avaliação é a clareza do limite e a redução do impacto de mudança, não o nome do padrão.

## PedidoService com responsabilidades excessivas

`PedidoService` carrega cliente e restaurante, procura o endereço, consulta localização, verifica área atendida, calcula taxa, busca e valida produtos, cria itens, persiste pedido, confirma estado, envia e-mail diretamente e encaminha pagamento. Seu construtor evidencia o acoplamento com quatro repositórios, `LocalizacaoService`, `PagamentoService` e `FakeEmailClient`.

Evidências e consequências:

- qualquer mudança em catálogo, localização, preço, confirmação, pagamento ou e-mail pode afetar a mesma classe;
- o serviço atravessa limites de várias funcionalidades;
- testes isolados tenderiam a exigir muitos colaboradores;
- alterações simples ganham amplo raio de impacto.

Possíveis refatorações incluem casos de uso menores, serviços de domínio específicos, módulos por funcionalidade, entrada de dados já resolvidos ou coordenação explícita. Deve-se evitar apenas dividir métodos mecanicamente em classes sem criar limites úteis.

## Dependências cruzadas entre funcionalidades

- `PagamentoController` chama `PedidoService` para pagar, mas usa `PagamentoService` para consultar.
- `PagamentoService` carrega e altera `Pedido`, persiste o pedido, notifica e cria `Entrega`.
- `EntregaService` navega por `Entrega -> Pedido -> Restaurante/Cliente/Endereco` e altera o status do pedido.
- `RestauranteService` também exerce papel de serviço de produto.
- `PedidoService` acessa diretamente repositórios que pertencem conceitualmente a cliente, restaurante e catálogo.

Essas dependências tornam difícil evoluir uma funcionalidade sem conhecer as demais. Uma reorganização por funcionalidade pode tornar dependências mais visíveis, mas só mover packages não resolve os fluxos cruzados; contratos e direção de dependência também precisam ser discutidos.

## Pagamento acoplado ao provedor e ao pedido

`PagamentoService` constrói `FakePaymentGateway.GatewayRequest`, interpreta os textos `AUTHORIZED` e `DECLINED` e salva `transactionCode`/`providerMessage` em `Pagamento`. A estrutura específica do provedor atravessa a regra de aplicação. O serviço também decide o novo estado do pedido, dispara notificação e cria a entrega.

O modelo `Pagamento` guarda campos úteis de auditoria, mas os nomes e significados refletem o único provedor atual. Isso torna substituição ou composição de gateways uma mudança transversal.

# Cenário de mudança: múltiplos gateways de pagamento

Impacto provável na versão atual:

- `PagamentoService` precisaria escolher gateway, construir diferentes requisições e interpretar vários status;
- `FakePaymentGateway` deixaria de ser o único formato externo;
- `PedidoService` e `PagamentoController` possivelmente receberiam um identificador de provedor;
- `Pagamento` e `ApiDtos` poderiam mudar para armazenar/expor códigos diferentes;
- testes precisariam cobrir seleção e tradução de cada fornecedor.

Possíveis melhorias:

- definir uma capacidade interna de processar pagamento com request/resultado próprios;
- criar adapters por provedor e selecionar por configuração ou regra de negócio;
- separar coordenação do pedido, processamento e efeitos após o resultado;
- traduzir status externos em um ponto de borda;
- adotar Strategy somente se a variação real justificar, evitando uma interface sem propósito.

## Notificação espalhada

Há três formas de chegar ao mesmo `FakeEmailClient`:

- `PedidoService` envia diretamente “Pedido confirmado”;
- `PagamentoService` chama `NotificacaoService` para aprovado/rejeitado;
- `EntregaService` envia diretamente “Entrega iniciada”.

Portanto, o canal e o formato de mensagem não têm proprietário único. `NotificacaoService` não encapsula todas as notificações e o fake concreto está injetado em mais de um serviço.

# Cenário de mudança: múltiplos canais de notificação

Para adicionar e-mail, SMS e aplicativo, a estrutura atual tende a exigir mudanças em `PedidoService`, `PagamentoService`, `NotificacaoService`, `EntregaService`, `FakeEmailClient`, composição Spring e possivelmente DTOs/preferências de `Cliente`. Também seria necessário decidir se todos os eventos usam todos os canais, como tratar falhas e se o envio é síncrono.

Possíveis melhorias:

- centralizar templates e seleção de canais em um componente de notificação;
- definir um contrato de canal e implementações por e-mail, SMS e push;
- publicar eventos simples de aplicação e ter handlers de notificação;
- registrar preferências por cliente e resolver canais fora dos serviços de pedido/entrega;
- manter chamadas explícitas, mas depender de uma fachada de notificação com operações semânticas.

Eventos não são obrigatoriamente a melhor solução: introduzem ordem, política de falha e rastreabilidade que precisam ser tratadas.

## Persistência e modelo

As entidades JPA também carregam regras de localização e estado. Serviços e controladores navegam pelo grafo persistido, por exemplo `entrega.getPedido().getCliente().getEmail()`. Isso acopla casos de uso ao formato das associações JPA e dificulta reduzir os dados que cada funcionalidade conhece.

O uso de `FetchType.EAGER` em endereços e itens foi escolhido para manter a API didática previsível com `open-in-view=false`, mas pode trazer carregamento excessivo conforme o sistema crescer. É outro trade-off válido para discussão, não o problema central.

## Testes como rede de segurança

Os testes unitários protegem cálculos, invariantes, transições de status e comportamento dos fakes. Os testes REST Assured verificam resultados HTTP, estados persistidos e notificações observáveis. Eles evitam verificar ordem exata de chamadas ou tipos de dependência, permitindo mover packages, substituir serviços e reorganizar responsabilidades desde que o comportamento externo seja mantido.

Ao refatorar, é aceitável alterar testes estritamente unitários que criam entidades diretamente quando o próprio limite do domínio mudar; os testes de API devem permanecer como proteção principal do comportamento observável.

# Mapa de problemas

| Problema | Local | Evidência | Conceito arquitetural | Mudança que evidencia o problema | Possíveis refatorações |
|---|---|---|---|---|---|
| Responsabilidades de localização espalhadas | `Cliente`, `Restaurante`, `Pedido`, `Entrega`, services, utils e `FakeMapsClient` | Distância, área, taxa e tempo têm vários donos | Coesão, separação de responsabilidades, limite arquitetural | Rota + região + horário | Módulo de localização, serviço de rota, política de taxa, objetos de valor |
| Lógica geográfica duplicada | Entidades, `DistanciaUtil`, `TaxaEntregaUtil` | Haversine, aproximação cartesiana e fator de rota; três fórmulas de taxa | Duplicação, fonte única de verdade | Ajuste do algoritmo ou preço | Consolidar cálculo e parametrizar política |
| Serviço com responsabilidades excessivas | `PedidoService` | Sete dependências e coordenação de cadastro, catálogo, localização, preço, pedido, pagamento e e-mail | SRP, coesão, acoplamento | Nova validação de produto ou modalidade de entrega | Casos de uso menores, serviços de domínio, módulos funcionais |
| Dependências cruzadas | `PedidoService`, `PagamentoService`, `EntregaService`, controllers | Pagamento altera pedido e cria entrega; entrega altera pedido | Direção de dependência, limites entre funcionalidades | Mudar ciclo de vida de pagamento/entrega | Orquestrador, eventos, contratos entre módulos, entradas mínimas |
| Dependência de detalhe externo | `LocalizacaoService`, `EntregaService`, `PagamentoService` | Uso direto de `FakeMapsClient.*` e `FakePaymentGateway.*` | Dependency Inversion, Adapter | Trocar mapas ou provedor de pagamento | Portas internas e adapters na borda |
| Estrutura do provedor vaza | `Pagamento`, `PagamentoService` | Status textual, código e mensagem do gateway dirigem regra interna | Anti-corruption layer, encapsulamento | Segundo gateway com status diferente | Resultado interno normalizado, tradução por adapter |
| Notificação espalhada | `PedidoService`, `NotificacaoService`, `EntregaService` | Chamadas diretas e indiretas ao e-mail concreto | Coesão, OCP, acoplamento | Adicionar SMS e push | Fachada, canais, eventos/handlers, preferências |
| Organização apenas por camadas técnicas | Todos os packages principais | Uma funcionalidade exige navegar por controller/service/repository/model | Modularidade, package by feature | Extrair ou evoluir somente pagamento | Reorganização por funcionalidade com limites internos |
| Navegação profunda no grafo JPA | `EntregaService`, DTOs e serviços | Acesso a `Pedido -> Cliente/Restaurante/Endereco` | Lei de Demeter, acoplamento ao modelo de persistência | Alterar associações ou separar módulos | Projeções, dados mínimos, objetos de comando/resultado |
| Produto sem limite próprio | `RestauranteService`, `ProdutoController` | Serviço de restaurante cadastra, consulta e altera produto | Coesão e limite funcional | Catálogo com estoque, opções e preços | Módulo/serviço de catálogo quando a complexidade justificar |
| Política de erro central e ampla | `ApiExceptionHandler` | Exceções genéricas de domínio e infraestrutura podem receber o mesmo 422 | Fronteira API, classificação de falhas | Novas integrações e erros transitórios | Hierarquia de erros por caso de uso e mapeamento explícito |

## Critérios sugeridos para avaliar a refatoração

- comportamento REST e estados permanecem iguais;
- `mvn clean verify` continua verde e com pelo menos 80% de cobertura de linhas;
- o raio de impacto dos três cenários de mudança diminui;
- dependências entre funcionalidades ficam explícitas e direcionadas;
- detalhes de provedor deixam de orientar regras centrais;
- duplicações relevantes são removidas sem criar abstrações para cada classe;
- a solução permanece compreensível para o tamanho do sistema.
