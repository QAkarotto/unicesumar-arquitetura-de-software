# Validação — Atividade 05 do 2º bimestre

Ambiente: Temurin JDK 26.0.2.1 e Maven 3.9.11. Execução em 25/11/2026, horário de Brasília.

Comando do projeto:

```sh
mvn clean verify
```

No ambiente foram utilizados também `-s /workspace/tools/settings.xml --batch-mode --no-transfer-progress -Dmaven.repo.local=/workspace/tools/m2`, para proxy e cache locais, e o repositório de certificados confiáveis do sistema. Essas configurações não alteram a aplicação.

| Verificação | Base `c8eab12` | Atividade 05 |
| --- | --- | --- |
| Testes unitários | 29 | 29 |
| Testes de API/integração | 11 | 17 |
| Falhas/erros/ignorados | 0/0/0 | 0/0/0 |
| Linhas cobertas no relatório | 700/715 — 97,90% | 711/726 — 97,93% |
| Resultado | BUILD SUCCESS | BUILD SUCCESS |

Contagens extraídas dos XMLs em `target/surefire-reports` e `target/failsafe-reports`. Cobertura calculada a partir do contador `LINE` de `target/site/jacoco/jacoco.xml`. O mínimo de 80% permanece configurado, sem novas exclusões no JaCoCo.

Trecho da execução:

```text
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] Analyzed bundle 'foodnow' with 74 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
[INFO] Total time:  18.615 s
```

## Evidências dos registros

Trechos reais produzidos durante a suíte, com prefixos de data/thread omitidos:

```text
INFO operacao=notificarPagamento pedidoId=1 status=APROVADO resultado=ENVIADA tentativas=1
INFO operacao=notificarPagamento pedidoId=2 status=REJEITADO resultado=ENVIADA tentativas=1
WARN operacao=notificarPagamento pedidoId=3 status=APROVADO tipoFalha=EmailIndisponivelException resultado=ENVIO_INCERTO tratamento=CONTINUAR_SEM_REENVIO tentativas=1
WARN operacao=notificarPagamento pedidoId=4 status=REJEITADO tipoFalha=EmailIndisponivelException resultado=ENVIO_INCERTO tratamento=CONTINUAR_SEM_REENVIO tentativas=1
WARN operacao=notificarPagamento pedidoId=5 status=APROVADO tipoFalha=EmailIndisponivelException resultado=ENVIO_INCERTO tratamento=CONTINUAR_SEM_REENVIO tentativas=1
WARN operacao=notificarPagamento pedidoId=6 status=REJEITADO tipoFalha=EmailIndisponivelException resultado=ENVIO_INCERTO tratamento=CONTINUAR_SEM_REENVIO tentativas=1
```

Os pedidos 3 e 4 reproduzem falha antes do envio; os pedidos 5 e 6 reproduzem aceitação seguida de falha na confirmação. Os identificadores acima pertencem à execução registrada e podem variar em outra execução. Os testes utilizam o ID retornado pela API nas suas verificações.

O teste confirma o log e consulta os registros por HTTP após a requisição de pagamento. Nos seis cenários, o pagamento retorna 201 e continua consultável com seu status. O pedido termina em `PAGO` ou `PAGAMENTO_REJEITADO`; a entrega existe apenas no primeiro caso. Cada envio de pagamento é chamado uma única vez. Quando o provedor aceita antes de falhar, há exatamente uma notificação de pagamento armazenada, sem duplicação.

O teste também verifica ausência de token, destinatário, conteúdo do e-mail e marcador sensível inserido na mensagem da exceção. O nível INFO é habilitado para essa classe na configuração do novo teste, permitindo capturar a evidência de sucesso sem alterar a configuração de produção.

Os 11 testes de API originais e os arquivos de referência foram preservados. A comparação com a base confirmou ausência de mudanças em controllers, DTOs, `pom.xml` e pipeline.
