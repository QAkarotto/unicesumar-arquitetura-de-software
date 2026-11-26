# Atividade 03: Arquitetura Web: Frontend, Backend e APIs

**Ferramenta:** draw.io (diagrams.net)
**Modalidade:** individual ou dupla
**Entrega:** Pull Request no repositório da disciplina

## Objetivo

Representar e analisar o fluxo de comunicação entre usuário, frontend, backend e dados a partir de uma API REST simulada, diferenciando as responsabilidades de cada parte da arquitetura Web.

## Cenário

A FoodNow disponibiliza uma aplicação para consulta de restaurantes. O frontend é responsável pela interação com o usuário e o backend disponibiliza uma API REST para consulta dos dados.

* **Frontend:** React / TypeScript
* **Backend:** Java / Spring Boot
* **Banco de dados:** PostgreSQL
* **Comunicação:** REST / HTTPS / JSON

## API REST simulada

Não será necessário realizar requisições reais. Utilize os exemplos abaixo para compreender o que o frontend envia e o que recebe.

### Consultar restaurantes

```http
GET /api/restaurants?latitude=-25.095&longitude=-50.161
Accept: application/json
```

**Resposta — 200 OK:**

```json
{
  "restaurants": [
    {
      "id": 1,
      "name": "Restaurante A",
      "distanceKm": 1.2
    },
    {
      "id": 2,
      "name": "Restaurante B",
      "distanceKm": 2.4
    }
  ]
}
```

### Consultar restaurante

```http
GET /api/restaurants/{id}
```

**Exemplo:**

```http
GET /api/restaurants/1
```

**Resposta — 200 OK:**

```json
{
  "id": 1,
  "name": "Restaurante A",
  "address": "Rua Exemplo, 100",
  "rating": 4.7
}
```

## Identificação das responsabilidades

Classifique as ações abaixo como **Frontend** ou **Backend**. Considere a ação específica, e não a funcionalidade como um todo.

| Ação                                        | Responsável |
| ------------------------------------------- | ----------- |
| Capturar a localização do dispositivo       | Frontend    |
| Enviar latitude e longitude para a API      | Frontend    |
| Receber a requisição de consulta            | Backend     |
| Consultar os restaurantes no banco de dados | Backend     |
| Montar a resposta da API                    | Backend     |
| Retornar os dados em JSON                   | Backend     |
| Receber a resposta JSON                     | Frontend    |
| Interpretar os dados recebidos              | Frontend    |
| Exibir os restaurantes na interface         | Frontend    |

## Diagrama de sequência

Crie no draw.io um diagrama de sequência, sem necessidade de seguir rigorosamente a notação UML, representando a consulta de restaurantes.

### Inclua, no mínimo:

* Usuário;
* Frontend;
* API / Backend;
* Banco de dados;
* requisições e respostas;
* método HTTP e formato dos dados;
* principais ações executadas em cada etapa.

### Como referência

```text
Usuário
  ↓
Frontend
  ↓ GET /api/restaurants
Backend
  ↓ consulta
PostgreSQL
  ↓ dados
Backend
  ↓ 200 OK + JSON
Frontend
  ↓
Exibição dos restaurantes
  ↓
Usuário
```

Utilize nomes e ícones das tecnologias quando forem relevantes para a representação.

[**Exemplo de diagrama de sequência**](https://creately.com/diagram/example/koIXYKnQau5/processo-de-pagamento-diagrama-de-sequencia)

## Arquitetura inadequada

Considere a seguinte solução:

```text
Frontend → PostgreSQL
Frontend → PayPal
```

Explique por que essa organização é inadequada e represente no draw.io uma versão corrigida, na qual o frontend se comunica com o backend e o backend é responsável pelo acesso aos dados e pelas integrações externas.

## Entrega

Envie o arquivo editável **`.drawio`** por meio de um **Pull Request** no repositório da disciplina.

No Pull Request, informe os integrantes da dupla, quando aplicável, e responda:

* Qual é a principal diferença entre as responsabilidades do frontend e do backend?
* Por que o frontend não deve acessar diretamente o banco de dados?
* Qual é o papel da API na comunicação entre frontend e backend?
