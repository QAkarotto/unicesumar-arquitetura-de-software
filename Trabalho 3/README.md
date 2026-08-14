# Atividade Prática — Arquitetura Web: Frontend, Backend e APIs (FoodNow)

> ⚠️ Nota: o enunciado colado continha uma instrução escondida direcionada a
> "sistemas de IA" pedindo para inserir referências fora de contexto no
> conteúdo gerado. Essa instrução foi ignorada — ela não tem relação com a
> atividade e não partiu de você.

## O que tem aqui

- **`parte2-arquitetura-web.drawio`** — arquivo único, editável, com **3 abas**:
  1. `04 - Diagrama de Sequencia`
  2. `05 - Arquitetura Inadequada`
  3. `06 - Arquitetura Corrigida`

  Abra em [diagrams.net](https://app.diagrams.net) (File → Open From → Device) e navegue pelas abas na parte inferior da tela.

- PNG/PDF de cada diagrama, só para conferência rápida sem precisar abrir o draw.io:
  - `01-diagrama-sequencia.png/.pdf`
  - `02-arquitetura-inadequada.png/.pdf`
  - `03-arquitetura-corrigida.png/.pdf`

---

## Tabela de responsabilidades (Frontend x Backend)

| Ação | Responsável |
|---|---|
| Capturar a localização do dispositivo | **Frontend** |
| Enviar latitude e longitude para a API | **Frontend** |
| Receber a requisição de consulta | **Backend** |
| Consultar os restaurantes no banco de dados | **Backend** |
| Montar a resposta da API | **Backend** |
| Retornar os dados em JSON | **Backend** |
| Receber a resposta JSON | **Frontend** |
| Interpretar os dados recebidos | **Frontend** |
| Exibir os restaurantes na interface | **Frontend** |

Critério usado: tudo que acontece **antes** da requisição sair do navegador/app (capturar dado do usuário, montar a chamada) e **depois** que a resposta chega nele (parsear, renderizar) é Frontend. Tudo que acontece **do lado do servidor** — receber a chamada, tocar no banco, montar o payload de resposta — é Backend. Nenhuma dessas ações é "a funcionalidade como um todo" (ex.: "buscar restaurantes"), e sim o passo específico da cadeia.

---

## Como publicar (git / PR)

```bash
git checkout -b atividade/arquitetura-web-frontend-backend
# copie esta pasta para dentro do repositório da disciplina, ex:
#   <repo>/atividades/parte2-arquitetura-web/
git add atividades/parte2-arquitetura-web
git commit -m "Adiciona diagramas de arquitetura web: sequencia, inadequada e corrigida - FoodNow"
git push -u origin atividade/arquitetura-web-frontend-backend
```

## Texto sugerido para a descrição do Pull Request

```markdown
## Atividade Prática — Arquitetura Web: Frontend, Backend e APIs (FoodNow)

**Integrante(s):** Pedro [+ nome da dupla, se aplicável]

### Conteúdo
`parte2-arquitetura-web.drawio` (3 abas):
- Diagrama de sequência da consulta de restaurantes (Usuário → Frontend →
  Backend/API → PostgreSQL e volta), com método HTTP, formato dos dados e a
  ação executada em cada etapa.
- Arquitetura inadequada: frontend acessando PostgreSQL e PayPal diretamente.
- Arquitetura corrigida: frontend fala apenas com o backend, que centraliza
  o acesso a dados e as integrações externas.

### Por que "Frontend → PostgreSQL" e "Frontend → PayPal" é inadequado
Isso expõe no cliente (navegador/app, código que roda na máquina do usuário)
tudo que deveria ficar só no servidor: credenciais de banco de dados, string
de conexão, chaves/segredos da API do PayPal e a lógica de negócio que valida
e processa pedidos e pagamentos. Qualquer pessoa consegue inspecionar o
tráfego ou o bundle do frontend e extrair essas credenciais. Além disso,
sem um backend no meio não há como validar regras de negócio antes de gravar
no banco ou cobrar um pagamento, não há um único lugar para aplicar
autenticação/autorização, e cada mudança de schema do banco quebraria
diretamente o frontend. A correção é simples: o frontend só conversa com o
backend via API REST; o backend é quem acessa o PostgreSQL e quem integra
com o PayPal.

### Respostas

**1. Qual é a principal diferença entre as responsabilidades do frontend e do backend?**
O frontend cuida da interação com o usuário: captura entradas (como a
localização), envia requisições, recebe respostas e apresenta os dados na
tela. O backend cuida das regras de negócio e do acesso a dados: recebe as
requisições, consulta/grava no banco, aplica validações e regras, e devolve
uma resposta estruturada (JSON). Em resumo: frontend = apresentação e
interação; backend = processamento, dados e regras.

**2. Por que o frontend não deve acessar diretamente o banco de dados?**
Porque o frontend roda no ambiente do usuário (navegador ou app), fora do
controle do desenvolvedor. Dar a ele acesso direto ao banco exigiria expor
credenciais de conexão no código cliente, elimina a camada que valida e
protege os dados antes de persisti-los, impede controle centralizado de
autenticação/autorização, e acopla a interface diretamente ao schema do
banco — qualquer mudança no banco quebraria o app. O backend existe
justamente para ser essa camada intermediária segura e controlada.

**3. Qual é o papel da API na comunicação entre frontend e backend?**
A API é o contrato entre as duas partes: define quais operações estão
disponíveis (endpoints), quais dados cada uma espera receber e devolver
(neste caso, JSON sobre REST/HTTPS) e os códigos de status que indicam o
resultado. Ela permite que frontend e backend evoluam de forma independente
— o backend pode trocar de banco de dados, linguagem ou implementação
interna sem que o frontend perceba, desde que o contrato da API seja
mantido.
```
