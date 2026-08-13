# Atividade 02: Diagrama arquitetural a nível de sistema, subsistema e componente

**Ferramenta:** draw.io (diagrams.net)
**Modalidade:** individual ou dupla
**Entrega:** Pull Request no repositório da disciplina

## Objetivo

Representar a arquitetura de um ecossistema de aplicações em diferentes níveis de abstração, identificando seus sistemas, subsistemas, componentes, tecnologias e principais relações.

## Cenário — FoodNow

A **FoodNow** é uma plataforma de delivery de alimentos composta por três aplicações:

* **Aplicativo do Cliente:** consulta restaurantes, realiza pedidos e acompanha entregas.
* **Portal do Restaurante:** administra cardápios e pedidos.
* **Aplicativo do Entregador:** consulta e atualiza entregas.

As aplicações utilizam um backend desenvolvido em Java com Spring Boot. **Nenhuma aplicação cliente acessa diretamente o banco de dados.**

## Tecnologias

| Elemento                 | Tecnologia               |
| ------------------------ | ------------------------ |
| Aplicativo do Cliente    | Flutter / Dart           |
| Portal do Restaurante    | React / TypeScript       |
| Aplicativo do Entregador | Flutter / Dart           |
| Backend / API            | Java / Spring Boot       |
| Banco de Dados           | PostgreSQL               |
| Cache                    | Redis                    |
| Comunicação              | REST / HTTP(S) / JSON    |
| Autenticação             | JWT                      |
| Pagamentos               | PayPal API               |
| Mapas e localização      | Google Maps Platform API |

> **Representação tecnológica:** utilize os ícones ou logos das tecnologias nos diagramas, acompanhados de seus respectivos nomes. O recurso visual deve facilitar a identificação da tecnologia utilizada em cada elemento arquitetural.

## Arquitetura do backend

O backend da FoodNow é organizado nos seguintes subsistemas:

* **Usuários e Autenticação:** cadastro, login e controle de acesso.
* **Pedidos:** criação, consulta e atualização dos pedidos.
* **Entregas:** gerenciamento das entregas e localização dos entregadores.

O subsistema de **Pedidos** utiliza PostgreSQL, Redis e a PayPal API.

O subsistema de **Entregas** utiliza PostgreSQL e a Google Maps Platform API.

## Diagramas

### Nível de Sistema

Represente a FoodNow em seu contexto, incluindo:

* Cliente;
* Restaurante;
* Entregador;
* FoodNow;
* PayPal;
* Google Maps Platform.

O diagrama deve representar as **fronteiras do sistema** e suas principais interações, sem detalhar os subsistemas internos.

### Nível de Subsistema

Detalhe a organização interna da FoodNow, representando:

* Aplicativo do Cliente;
* Portal do Restaurante;
* Aplicativo do Entregador;
* Backend/API;
* Usuários e Autenticação;
* Pedidos;
* Entregas;
* PostgreSQL;
* Redis;
* PayPal;
* Google Maps Platform.

Represente as principais comunicações, incluindo a utilização de **REST, HTTP/HTTPS e JSON**.

### Nível de Componente

Detalhe o subsistema **Pedidos**, representando seus principais componentes internos.

Como referência, podem ser utilizados:

* Controller de Pedidos;
* Serviço de Pedidos;
* Validador de Pedido;
* Repositório de Pedidos;
* Integração com PayPal;
* PostgreSQL.

Os componentes devem indicar suas respectivas tecnologias quando aplicável.

> **Coerência arquitetural:** os três diagramas devem representar o mesmo sistema em diferentes níveis de abstração. O diagrama de subsistemas deve detalhar o diagrama de sistema, e o diagrama de componentes deve detalhar o subsistema **Pedidos**.

## Entrega

Crie uma pasta específica para a atividade no repositório da disciplina.

Inclua os três diagramas exportados em **PDF ou PNG** e, preferencialmente, também o arquivo editável **`.drawio`**.

Crie uma branch, realize o commit dos arquivos e abra um **Pull Request** para o repositório da disciplina.

No Pull Request, informe:

* os integrantes da dupla, quando aplicável;
* uma decisão arquitetural considerada relevante;
* uma breve reflexão sobre os diferentes níveis de abstração utilizados.

## Conceito central

Os diagramas representam o mesmo sistema em diferentes níveis de detalhe:

* **Nível de sistema:** apresenta o **contexto e as fronteiras**;
* **Nível de subsistema:** apresenta a **organização interna**;
* **Nível de componente:** apresenta a **estrutura interna de uma parte do sistema**.

### Referências

[Diagrama simples demonstrado na aula](https://excalidraw.com/#json=DdgAenUGU0ogwuxVv5HN5,gAWzzvgfHswnICEBnb7Dvg)

> **Observação:** o diagrama acima é simples e serve apenas para exemplificar a ideia. Os diagramas da atividade devem ser mais completos.

#### Outros exemplos

* Diagrama de arquitetura — Creately:
  https://svg.template.creately.com/mGFFzhlRr2h

* Slack Architecture — AWS Architecture Diagram Template:
  https://online.visual-paradigm.com/repository/images/b0ab820f-e4ca-4e39-99a8-0240048b6539/aws-architecture-diagram-design/slack-architecture.png
