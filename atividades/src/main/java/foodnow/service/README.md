# Modularidade, Coesão e Acoplamento no FoodNow

## Integrantes

Victor Gabriel Alves Carneiro
Felipe Augusto Graniska
---

# Problemas identificados

Durante a análise foi observado que as responsabilidades relacionadas à entrega estavam distribuídas entre diferentes serviços.

Principais problemas encontrados:

- `PedidoService` realizava cálculos de taxa, tempo e verificação da área de entrega.
- `EntregaService` também realizava cálculos de distância, taxa e tempo.
- `LocalizacaoService` concentrava regras de localização e regras de negócio da entrega.
- Existia duplicação de responsabilidades entre os serviços.
- Alterações futuras exigiriam modificações em várias classes.

---

# Fluxo de dependências

```text
Criar Pedido
      ↓
PedidoService
      ↓
PagamentoService
      ↓
EntregaService
      ↓
PoliticaEntregaService
      ↓
LocalizacaoService
      ↓
NotificacaoService
```

---

# Decisão arquitetural

Foi criada a classe `PoliticaEntregaService` para centralizar todas as regras relacionadas à entrega.

Antes da refatoração, essas regras estavam distribuídas entre `PedidoService`, `EntregaService` e `LocalizacaoService`.

Após a refatoração, os cálculos de taxa, tempo, área de entrega e demais regras passaram a ser executados por um único serviço, reduzindo o acoplamento e aumentando a coesão.

---

# Principais alterações

## PoliticaEntregaService

Responsável por:

- Calcular distância;
- Calcular taxa de entrega;
- Calcular tempo de entrega;
- Verificar área de atendimento;
- Definir região.

## PedidoService

Antes:

- Criava pedidos;
- Calculava taxa de entrega;
- Estimava tempo de entrega;
- Verificava área de entrega.

Depois:

- Criar pedido;
- Confirmar pedido;
- Cancelar pedido;
- Consultar status;
- Calcular valor total.

## EntregaService

Antes:

- Criava entrega;
- Calculava distância;
- Calculava taxa;
- Calculava tempo.

Depois:

- Criar entrega;
- Consultar entrega;
- Atualizar status da entrega.

## LocalizacaoService

Antes:

- Calcular distância;
- Definir região;
- Calcular taxa;
- Calcular tempo;
- Verificar área.

Depois:

- Calcular distância;
- Definir região.

---

# Comparação

## Antes

```text
PedidoService
 ├── Pedido
 ├── Taxa
 ├── Tempo
 └── Área

EntregaService
 ├── Distância
 ├── Taxa
 └── Tempo

LocalizacaoService
 ├── Distância
 ├── Região
 ├── Taxa
 ├── Tempo
 └── Área
```

## Depois

```text
PedidoService
 └── Pedido

EntregaService
 └── Entrega

LocalizacaoService
 ├── Distância
 └── Região

PoliticaEntregaService
 ├── Distância
 ├── Taxa
 ├── Tempo
 ├── Área
 └── Região
```

---

# Benefícios obtidos

A refatoração proporcionou:

- Redução do acoplamento entre os serviços;
- Aumento da coesão;
- Eliminação da duplicação de regras;
- Centralização das regras de entrega;
- Facilidade para futuras alterações relacionadas à rota, região, horário e condições operacionais.

---

# Problemas não tratados

Como o projeto foi desenvolvido em uma versão simplificada, ficaram fora do escopo:

- Banco de dados;
- Persistência de dados;
- API REST;
- Testes automatizados;
- Integrações externas.

---

# Trade-offs

A criação da classe `PoliticaEntregaService` adicionou um novo componente ao projeto, porém reduziu significativamente a quantidade de responsabilidades existentes nos demais serviços.

Com essa organização, futuras alterações nas regras de entrega poderão ser implementadas em um único local, diminuindo o impacto de manutenção e facilitando a evolução do sistema.