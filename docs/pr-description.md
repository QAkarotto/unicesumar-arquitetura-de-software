# Atividade 01 — observação opcional nos pedidos

## Integrantes

- Felipe de Oliveira Guimarães
- Otavio Alves Brisky

Turma: **ESOFT8S**.

## Problema e implementação

Pedidos agora aceitam `observacao` na criação e mantêm o texto na persistência
e nas respostas de criação, consulta, adição de itens e confirmação.
Exemplo: `"observacao": "Não incluir talheres"`.

- Campo opcional com `@CodePointLength(max = 500)`; 500 pontos de código Unicode aceitos, 501 rejeitados com HTTP 400 / `VALIDATION_ERROR`, inclusive com emojis e caracteres suplementares.
- Ausência e `null` armazenam `null` e omitem a propriedade no JSON de resposta; vazio e espaços são preservados.
- Desserialização específica impede converter números e booleanos em texto. Números, booleanos, objetos e arrays retornam HTTP 400 pelo tratamento de leitura do Spring MVC.
- Coluna JPA anulável com capacidade para 1000 unidades UTF-16, comportando 500 pontos de código suplementares; o limite da API continua 500 pontos de código. `updatable = false`, sem setter ou endpoint de edição. O construtor anterior da entidade foi preservado.
- `PedidoResponse.from` expõe o novo campo nas quatro operações; `@JsonInclude(NON_NULL)` mantém a omissão de nulos também no contexto de testes.
- Nenhuma alteração nas regras de itens, valores, pagamento ou entrega, nas dependências ou na configuração de cobertura.

## Contrato e compatibilidade

Comparação em [analise-evolucao-contrato.md](docs/analise-evolucao-contrato.md).
Especificações OpenAPI 3.0.3: [antes](docs/openapi-before.yaml) e
[depois](docs/openapi-after.yaml).

A evolução é aditiva para clientes que enviam o contrato anterior e aceitam
propriedades adicionais nas respostas. Pedidos sem observação preservam o JSON
anterior, inclusive suas comparações exatas. Não foi criada `/v2`, porque não
houve remoção ou mudança de significado das operações anteriores.

Clientes externos com schemas fechados podem precisar de atualização ao
receber pedidos com observação; são uma possibilidade, não consumidores
identificados. Os consumidores HTTP encontrados são os testes RestAssured do
repositório. As condições e limites da compatibilidade estão na análise.

## Testes e cobertura — execução concluída

Comando PowerShell executado em 23/10/2026:

```powershell
mvn --batch-mode --no-transfer-progress "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean verify
```

O parâmetro seleciona o cache Maven existente do usuário, sem modificar
configurações do projeto.

Resultado final:

```text
Unitários: Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
API:       Tests run: 31, Failures: 0, Errors: 0, Skipped: 0
All coverage checks have been met.
BUILD SUCCESS
```

São **60 testes**, incluindo **20 execuções** em `PedidoObservacaoIT`.
Cobrem observação informada, ausência, `null`, vazio, espaços, 500/501 pontos
de código Unicode com texto comum, emojis, caracteres suplementares não emoji e texto misto,
seis casos de valores JSON inválidos, persistência verificada diretamente por JDBC,
recuperação por GET, manutenção após adicionar itens e confirmar e tentativas
de edição nas operações posteriores. Os testes anteriores e os arquivos de
referência de geografia foram preservados integralmente.

Cobertura extraída dos contadores `LINE` de `target/site/jacoco/jacoco.xml`:

| Medição | Linhas cobertas | Não cobertas | Cobertura |
|---|---:|---:|---:|
| Relatório geral | 707 | 16 | **97,79%** |
| Escopo do check, com a exclusão de `FoodNowApplication` já existente | 706 | 14 | **98,06%** |

O limite de **80%** foi aprovado pelo próprio `jacoco:check`, sem novas
exclusões. Relatório HTML local: `target/site/jacoco/index.html`.

## Validação OpenAPI — execução concluída

Python **3.14.5**, `openapi-spec-validator` **0.9.0**, instalado em ambiente
virtual temporário, fora do projeto. Comandos PowerShell para reproduzir:

```powershell
python -m venv "$env:TEMP\foodnow-openapi-validator"
& "$env:TEMP\foodnow-openapi-validator\Scripts\python.exe" -m pip install openapi-spec-validator==0.9.0
& "$env:TEMP\foodnow-openapi-validator\Scripts\python.exe" -m openapi_spec_validator docs/openapi-before.yaml docs/openapi-after.yaml
```

Saída completa da validação final (código de saída **0**):

```text
docs/openapi-before.yaml: OK
docs/openapi-after.yaml: OK
```

A ferramenta validou a leitura YAML e a estrutura OpenAPI dos dois documentos.
Os comportamentos HTTP e de persistência foram verificados pelos testes de API.

O banco permanece H2 em memória com `create-drop`, como na base do professor.
O armazenamento demonstrado é entre requisições; não é durável após reiniciar.
