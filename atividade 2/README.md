# Diagramas de Arquitetura — FoodNow

Esta pasta contém os três diagramas pedidos na atividade, cada um em três formatos:

| Diagrama | Arquivo editável (draw.io) | Imagem | PDF |
|---|---|---|---|
| Nível de Sistema (contexto) | `01-nivel-sistema.drawio` | `01-nivel-sistema.png` | `01-nivel-sistema.pdf` |
| Nível de Subsistema | `02-nivel-subsistema.drawio` | `02-nivel-subsistema.png` | `02-nivel-subsistema.pdf` |
| Nível de Componente (subsistema Pedidos) | `03-nivel-componente-pedidos.drawio` | `03-nivel-componente-pedidos.png` | `03-nivel-componente-pedidos.pdf` |

Os `.drawio` abrem direto em [diagrams.net](https://app.diagrams.net) (File → Open From → Device). Os ícones de tecnologia estão representados como selos coloridos com sigla (FL = Flutter, RE = React, SB = Spring Boot, PG = PostgreSQL, RD = Redis, PP = PayPal, GM = Google Maps) — se quiser, dá pra trocar por logos oficiais usando a busca de formas (Shape Search) do próprio draw.io antes de exportar de novo.

---

## Como publicar (passo a passo no repositório da disciplina)

```bash
# 1. dentro do repositório da disciplina
git checkout -b atividade/diagramas-arquitetura-foodnow

# 2. copie esta pasta inteira para dentro do repo, ex:
#    <repo>/atividades/diagramas-arquitetura-foodnow/

git add atividades/diagramas-arquitetura-foodnow
git commit -m "Adiciona diagramas de arquitetura (sistema, subsistema, componente) - FoodNow"
git push -u origin atividade/diagramas-arquitetura-foodnow

# 3. abra o Pull Request no GitHub e cole a descrição abaixo
```

---

## Texto sugerido para a descrição do Pull Request

> Copie o bloco abaixo (ajuste "Iryane Karolyne Ienke" se a atividade for feita em dupla, ou remova se for individual).

```markdown
## Atividade Prática — Diagramas de Arquitetura de Software (FoodNow)

**Integrante(s):** Pedro [+ nome da dupla, se aplicável]

### Conteúdo
- `01-nivel-sistema.*` — diagrama de contexto: Cliente, Restaurante e Entregador
  interagindo com a FoodNow, e a FoodNow interagindo com PayPal e Google Maps
  Platform.
- `02-nivel-subsistema.*` — organização interna da FoodNow: os três front-ends
  (Aplicativo do Cliente, Portal do Restaurante, Aplicativo do Entregador) consumindo
  o Backend/API via REST/HTTPS/JSON, e o backend dividido nos subsistemas Usuários e
  Autenticação, Pedidos e Entregas, cada um com suas dependências (PostgreSQL, Redis,
  PayPal API, Google Maps Platform API).
- `03-nivel-componente-pedidos.*` — detalhamento interno do subsistema Pedidos:
  Controller → Service → (Validador | Repository → PostgreSQL | Integração PayPal →
  PayPal API).

### Decisão arquitetural relevante
Nenhuma aplicação cliente acessa o banco de dados diretamente — todo acesso a dados
passa pelo Backend/API em Spring Boot. Isso centraliza as regras de negócio e a
validação em um único ponto, evita duplicação de lógica entre o app Flutter e o
portal React, e permite trocar ou escalar o banco (PostgreSQL) sem impactar os
clientes. O custo dessa decisão é que o backend se torna um ponto único de
acoplamento: qualquer instabilidade nele afeta as três aplicações simultaneamente,
o que reforça a importância de subsistemas bem isolados (Autenticação, Pedidos,
Entregas) e do uso de cache (Redis) para reduzir a carga sobre o Pedidos.

### Reflexão sobre os níveis de abstração
O diagrama de sistema deixa claro *quem* participa do ecossistema e *por que* eles
se comunicam, sem expor como a FoodNow é construída internamente — é a visão que
interessaria a um stakeholder não-técnico. O diagrama de subsistema já assume uma
audiência técnica: mostra como o backend se decompõe em responsabilidades (Auth,
Pedidos, Entregas) e quais tecnologias e protocolos conectam cada peça, mas ainda
trata cada subsistema como uma caixa. O diagrama de componentes existe para quem
vai efetivamente programar dentro do subsistema Pedidos: mostra as classes/camadas
(Controller, Service, Validador, Repository, Integração PayPal) e o fluxo real de
uma requisição. Cada nível "abre" o anterior sem repetir informação desnecessária —
a progressão ajuda a comunicar a arquitetura para audiências diferentes sem
sobrecarregar nenhuma delas com detalhes que não lhe dizem respeito.
```
