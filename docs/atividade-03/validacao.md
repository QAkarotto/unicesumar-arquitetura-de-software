# Validação — Atividade 03 do 2º bimestre

Ambiente: Temurin JDK 26.0.2.1 e Maven 3.9.11. Execução em 25/11/2026, horário de Brasília.

Comando do projeto:

```sh
mvn clean verify
```

No ambiente de execução foram utilizados também `-s /workspace/tools/settings.xml --batch-mode --no-transfer-progress -Dmaven.repo.local=/workspace/tools/m2`, para proxy e cache locais, e o repositório de certificados confiáveis do sistema. Essas configurações não alteram o projeto.

| Verificação | Base `c8eab12` | Atividade 03 |
| --- | --- | --- |
| Testes unitários | 29 | 39 |
| Testes de API/integração | 11 | 11 |
| Falhas/erros/ignorados | 0/0/0 | 0/0/0 |
| Linhas cobertas no relatório | 700/715 — 97,90% | 707/722 — 97,92% |
| Resultado | BUILD SUCCESS | BUILD SUCCESS |

Contagens extraídas dos XMLs em `target/surefire-reports` e `target/failsafe-reports`. Cobertura calculada a partir do contador `LINE` de `target/site/jacoco/jacoco.xml`. O JaCoCo manteve o mínimo de 80%, sem novas exclusões.

Trechos da execução:

```text
[INFO] Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] Analyzed bundle 'FoodNow' with 75 classes
[INFO] Analyzed bundle 'foodnow' with 74 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
[INFO] Total time:  17.228 s
```

Os dez novos casos estão em `MapsRepeticaoTest`: três possibilidades de sucesso para rota, três para coordenadas, dois casos de esgotamento, um de erro não transitório e um de resposta malformada. As verificações incluem o número exato de chamadas e a exceção propagada.

A comparação com a base confirmou ausência de alterações nos testes de API, em `src/test/resources`, no `pom.xml` e no pipeline. Os testes de geografia continuam comparando as respostas completas com as referências originais.
