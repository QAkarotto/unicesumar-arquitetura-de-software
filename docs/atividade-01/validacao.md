# Evidências — Atividade 01 do 2º bimestre

Ambiente: Temurin JDK 26.0.2.1, Maven 3.9.11. Execução em 25/11/2026, horário de Brasília.

## Suíte Maven

Comando do projeto:

```sh
mvn clean verify
```

Neste ambiente, a execução utilizou também `-s /workspace/tools/settings.xml --batch-mode --no-transfer-progress -Dmaven.repo.local=/workspace/tools/m2`, apenas para configurar o proxy e o cache locais. O Maven utilizou o repositório de certificados confiáveis do sistema. Essas configurações não fazem parte do projeto e não alteram o `pom.xml`.

| Verificação | Base do professor | Atividade 01 |
| --- | --- | --- |
| Testes unitários | 29 | 30 |
| Testes de integração/API | 11 | 21 |
| Falhas/erros/ignorados | 0/0/0 | 0/0/0 |
| Cobertura de linhas do relatório | 700/715 — 97,90% | 706/721 — 97,92% |
| Resultado | BUILD SUCCESS | BUILD SUCCESS |

Contagens extraídas dos XMLs de `target/surefire-reports` e `target/failsafe-reports`. Cobertura calculada a partir do contador `LINE` do relatório `target/site/jacoco/jacoco.xml`. O critério JaCoCo do projeto permanece em 80%, com suas exclusões originais.

Trecho da execução final:

```text
[INFO] Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] Analyzed bundle 'FoodNow' with 74 classes
[INFO] Analyzed bundle 'foodnow' with 73 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
[INFO] Total time:  17.891 s
```

Todos os testes de API anteriores e as referências geográficas foram preservados. Não houve alteração do pipeline, das integrações simuladas ou das regras de valores, pagamento e entrega.

## Validação OpenAPI

Validador: `openapi-spec-validator` 0.7.2, instalado em ambiente Python isolado, sem dependência adicional na aplicação.

Para reproduzir a partir da raiz do projeto:

```sh
python3 -m venv /tmp/foodnow-openapi
/tmp/foodnow-openapi/bin/pip install openapi-spec-validator==0.7.2
/tmp/foodnow-openapi/bin/python -m openapi_spec_validator docs/atividade-01/openapi-antes.json
/tmp/foodnow-openapi/bin/python -m openapi_spec_validator docs/atividade-01/openapi.json
```

Resultado obtido para ambos os documentos:

```text
docs/atividade-01/openapi-antes.json: OK
docs/atividade-01/openapi.json: OK
```

O validador verifica a estrutura OpenAPI 3.0.3 e as referências dos documentos. A conformidade do comportamento HTTP com a decisão sobre observação foi verificada pelos testes de API; a validação estrutural sozinha não executa a aplicação.
