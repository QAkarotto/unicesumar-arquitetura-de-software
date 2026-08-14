# Arquitetura Web: Frontend, Backend e APIs — FoodNow

Atividade prática de representação e análise do fluxo de comunicação entre usuário, frontend, backend e banco de dados a partir de uma API REST simulada.

**Integrante(s):** Lucas ([@LuquetaW](https://github.com/LuquetaW))

## Arquivo entregue

`foodnow-arquitetura-web.drawio` — arquivo editável do draw.io, organizado em três páginas:

1. **Diagrama de sequência** — consulta de restaurantes e consulta de um restaurante específico, com usuário, frontend, API/backend e banco de dados, incluindo método HTTP, formato dos dados e as ações executadas em cada etapa.
2. **Arquitetura inadequada** — o cenário em que o frontend acessa diretamente o PostgreSQL e a PayPal, com a análise dos problemas.
3. **Arquitetura corrigida** — o frontend conversa apenas com a API, e o backend concentra o acesso aos dados e as integrações externas.

## Cenário

| Camada | Tecnologia |
| --- | --- |
| Frontend | React / TypeScript |
| Backend | Java / Spring Boot |
| Banco de dados | PostgreSQL |
| Comunicação | REST / HTTPS / JSON |

### Endpoints utilizados

```http
GET /api/restaurants?latitude=-25.095&longitude=-50.161
Accept: application/json
```

```json
{
  "restaurants": [
    { "id": 1, "name": "Restaurante A", "distanceKm": 1.2 },
    { "id": 2, "name": "Restaurante B", "distanceKm": 2.4 }
  ]
}
```

```http
GET /api/restaurants/1
Accept: application/json
```

```json
{
  "id": 1,
  "name": "Restaurante A",
  "address": "Rua Exemplo, 100",
  "rating": 4.7
}
```

## Identificação das responsabilidades

| Ação | Responsável |
| --- | --- |
| Capturar a localização do dispositivo | Frontend |
| Enviar latitude e longitude para a API | Frontend |
| Receber a requisição de consulta | Backend |
| Consultar os restaurantes no banco de dados | Backend |
| Montar a resposta da API | Backend |
| Retornar os dados em JSON | Backend |
| Receber a resposta JSON | Frontend |
| Interpretar os dados recebidos | Frontend |
| Exibir os restaurantes na interface | Frontend |

O ponto que exige mais atenção na tabela está no par "capturar a localização" e "consultar os restaurantes". A funcionalidade é a mesma — encontrar restaurantes próximos — mas a captura acontece no dispositivo, via API de geolocalização do navegador, enquanto a consulta acontece no servidor, que é quem tem acesso ao banco.

## Respostas

### Qual é a principal diferença entre as responsabilidades do frontend e do backend?

O frontend existe para mediar a relação entre o sistema e a pessoa. Ele desenha a tela, captura o que o usuário faz, dispara a requisição e apresenta o que voltou. Já o backend cuida daquilo que não pode depender da máquina do usuário: validar a entrada, aplicar as regras de negócio, acessar o banco, guardar credenciais e decidir o que cada pessoa pode ou não ver. Uma forma curta de resumir é que o frontend é responsável pela experiência e o backend pela verdade — se os dois discordarem sobre um preço, uma permissão ou um resultado, quem vale é o servidor.

### Por que o frontend não deve acessar diretamente o banco de dados?

Porque o código do frontend roda no computador do usuário e está totalmente sob o controle dele. Qualquer credencial usada para abrir uma conexão com o PostgreSQL estaria visível no código-fonte ou no tráfego de rede, e bastaria alguém abrir as ferramentas de desenvolvedor para copiá-la. Além disso, o banco precisaria aceitar conexões vindas da internet aberta, algo que normalmente se evita mantendo-o em rede interna. Há ainda um problema de manutenção: quando a interface conhece tabelas e colunas, ela fica presa ao modelo de dados, e uma simples renomeação de coluna quebra a aplicação. Com o backend no meio, o banco fica isolado, as credenciais permanecem no servidor e o modelo interno pode evoluir sem afetar quem consome.

### Qual é o papel da API na comunicação entre frontend e backend?

A API é o contrato entre os dois lados. Ela define quais endereços existem, qual método HTTP usar, quais parâmetros são aceitos e qual o formato da resposta — no caso da FoodNow, `GET /api/restaurants` com latitude e longitude na query string, devolvendo JSON com status 200. Esse contrato permite que os dois lados evoluam de forma independente: o backend pode trocar de banco, reescrever a consulta ou mudar de gateway de pagamento, e enquanto o formato da resposta continuar o mesmo o frontend nem percebe. A API também é o ponto onde se concentram autenticação, autorização, controle de acesso e registro de log, e é o que torna viável atender vários clientes — web, iOS, Android — com uma única implementação das regras.
