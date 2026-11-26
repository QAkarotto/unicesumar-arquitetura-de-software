# Validação — Atividade 02 do 2º bimestre

Ambiente: Temurin JDK 26.0.2.1 e Maven 3.9.11. Execução em 25/11/2026, horário de Brasília.

Comando do projeto:

```sh
mvn clean verify
```

Neste ambiente foram utilizados também `-s /workspace/tools/settings.xml --batch-mode --no-transfer-progress -Dmaven.repo.local=/workspace/tools/m2`, para proxy e cache locais, e o repositório de certificados confiáveis do sistema. Nenhuma dessas configurações foi adicionada ao projeto.

| Verificação | Base `c8eab12` | Atividade 02 |
| --- | --- | --- |
| Testes unitários | 29 | 37 |
| Testes de API/integração | 11 | 11 |
| Falhas/erros/ignorados | 0/0/0 | 0/0/0 |
| Linhas cobertas no relatório | 700/715 — 97,90% | 711/725 — 98,07% |
| Resultado | BUILD SUCCESS | BUILD SUCCESS |

As contagens foram extraídas dos XMLs em `target/surefire-reports` e `target/failsafe-reports`. A cobertura foi calculada a partir do contador `LINE` de `target/site/jacoco/jacoco.xml`. O limite mínimo de 80% e as exclusões originais do JaCoCo foram preservados.

Trechos da execução:

```text
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Analyzed bundle 'FoodNow' with 77 classes
[INFO] Analyzed bundle 'foodnow' with 76 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
[INFO] Total time:  18.033 s
```

Os oito novos casos são quatro do serviço (aprovação, rejeição, pedido não confirmado e pedido inexistente) e quatro do adaptador (conversão da solicitação e três respostas de status). Eles não exigem comunicação externa.

A comparação com a base confirmou ausência de mudanças em `src/test/java/br/edu/foodnow/controller`, `src/test/resources`, `pom.xml` e `.github/workflows/ci.yml`. Nenhum endpoint ou DTO de API foi alterado. Os cenários HTTP completos e as referências geográficas continuam passando com o adaptador injetado pelo Spring.
