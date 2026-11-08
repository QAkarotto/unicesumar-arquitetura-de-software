# Atividade 05 — Resiliência e Observabilidade

## Cenário de falha e impacto

O fluxo selecionado é o cálculo de rota usado, entre outros pontos, durante a criação de pedido. `LocalizacaoService` coordena a chamada para `FakeMapsClient.calcularRota`. Uma indisponibilidade temporária ou timeout do provedor pode interromper o cálculo e impedir o fluxo que depende da rota.

O serviço já repetia falhas temporárias até três vezes, com espera crescente de 50 ms e 100 ms. Quando o limite era atingido, lançava `ServicoLocalizacaoIndisponivelException`. A limitação observada era operacional: não havia registro que indicasse qual operação falhou, quantas tentativas foram feitas ou se houve recuperação, dificultando distinguir recuperação temporária de indisponibilidade persistente.

## Melhoria implementada

Foi mantido o retry limitado existente e foi acrescentei logs de diagnóstico em `LocalizacaoService` para:

- cada falha temporária, com nome da operação, tentativa atual, limite e tipo da exceção;
- recuperação após retry, com operação e número de tentativas;
- esgotamento do limite ou interrupção durante a espera, com resultado final.

Os logs não incluem coordenadas, endereço, credenciais. A escolha mantém a política de resiliência e os contratos da API, ao mesmo tempo que torna o tratamento observável. O retry é adequado aqui porque as consultas de localização são leituras. O limite evita repetição indefinida, mas pode acrescentar até 150 ms de espera configurada; não resolve indisponibilidade prolongada e pode repetir uma chamada que ainda esteja em andamento se o adaptador real não tiver timeout. Um cliente HTTP deve configurar limites de conexão e leitura.

## Evidências automatizadas

O teste `deveRegistrarFalhaTemporariaERecuperacaoSemDadosDaOperacao` simula timeout na primeira chamada e sucesso na segunda. Ele confirma os logs de falha e recuperação e verifica que coordenadas não aparecem. O teste `deveFalharComServicoIndisponivelDepoisDoLimiteDeTentativas` simula indisponibilidade persistente, confirma três chamadas, a exceção final e o log de esgotamento.

Trechos dos testes:

```text
WARN  Falha transitória na integração de localização: operação=calcularRota, tentativa=1, limite=3, tipo=FalhaTemporariaMapasException
INFO  Integração de localização recuperada: operação=calcularRota, tentativas=2
WARN  Falha transitória na integração de localização: operação=calcularRota, tentativa=3, limite=3, tipo=FalhaTemporariaMapasException
ERROR Integração de localização indisponível após retries: operação=calcularRota, tentativas=3, tipo=FalhaTemporariaMapasException
```

## Testes

```text
[INFO] Unit tests: 31 run, 0 failures, 0 errors, 0 skipped
[INFO] Integration tests: 7 run, 0 failures, 0 errors, 0 skipped
[INFO] JaCoCo analyzed bundle 'FoodNow' with 74 classes
[INFO] JaCoCo analyzed bundle 'foodnow' with 73 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```
