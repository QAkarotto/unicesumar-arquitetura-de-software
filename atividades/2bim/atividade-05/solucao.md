# Atividade 05 — Resiliência e observabilidade no FoodNow

## Integrantes

- Eduardo Alan dos Santos — RA: 23074991-2
- Lucas Wessendorf de Araujo — RA: 23351656-2

## Fluxo escolhido e comportamento anterior

Escolhemos a consulta de rota usada na cotação do pedido. O componente responsável pela
integração é `MapsLocalizacaoAdapter`, que implementa `ProvedorLocalizacao` e utiliza
`FakeMapsClient`. O cliente de mapas continua simulado, sem depender de um serviço externo.

```text
PedidoService.criar → LogisticaService.cotarPedido
                   → MapsLocalizacaoAdapter.calcularRota → FakeMapsClient
```

O comportamento esperado é obter distância, prazo e zona de entrega para que a cotação
continue usando as regras existentes. Antes da alteração, o adapter fazia uma única chamada
e traduzia a resposta para `Rota`. Uma falha de comunicação era propagada imediatamente,
sem tentativa de recuperação nem registros próprios sobre a consulta afetada.

Na criação do pedido, essa falha interrompia a cotação antes do salvamento. Mesmo que o
provedor estivesse disponível logo depois, o cliente precisaria repetir a operação.

## Falha simulada

Simulamos a indisponibilidade temporária do provedor com Mockito, fazendo
`FakeMapsClient.calcularRota` lançar `ResourceAccessException`. Essa exceção representa um
erro de comunicação, como falha de conexão ou timeout em um cliente HTTP do Spring.

No cenário de recuperação, a primeira chamada falha e a segunda devolve a rota normal.
Também simulamos duas falhas antes do sucesso, indisponibilidade durante todas as tentativas
e um erro de dados que não deve ser repetido. A simulação fica nos testes; o cliente usado
na execução normal mantém seu comportamento determinístico.

## Melhoria implementada

Adicionamos um limite de **três tentativas no total**, incluindo a chamada inicial, somente
para `ResourceAccessException`. Quando uma chamada funciona, o adapter retorna imediatamente
a mesma tradução de distância, prazo e zona que já existia. Se as três falharem, registra o
esgotamento e propaga a exceção original. Outros erros são registrados e propagados na
primeira ocorrência, sem repetição.

Escolhemos tratar a falha no adapter porque ele já é a fronteira da integração. Os serviços
de negócio continuam dependendo de `ProvedorLocalizacao` e não precisam conhecer a política
de tentativas nem a exceção do cliente HTTP. A alteração de produção ficou restrita ao
método de cálculo de rota; geocodificação, regras de entrega, endpoints e DTOs foram preservados.

Consultar uma rota não grava pedidos nem realiza cobranças ou despachos. Por isso, repetir
essa consulta não duplica efeitos de negócio. Essa política não foi aplicada ao pagamento
nem ao envio de notificações, que exigiriam outra avaliação sobre possíveis efeitos duplicados.

## Registros de diagnóstico

Cada consulta recebe um `operacaoId` aleatório. Todas as suas tentativas usam o mesmo
identificador; consultas diferentes recebem identificadores diferentes.

| Campo | Informação registrada |
| --- | --- |
| `operacao` | `calcular_rota` |
| `operacaoId` | Identificador da consulta, usado para agrupar suas tentativas |
| `tentativa` e `limite` | Número da chamada e limite total de três |
| `tipoFalha` | Classe da exceção, quando há falha |
| `resultado` | `sucesso`, `nova_tentativa`, `tentativas_esgotadas` ou `falha_nao_repetivel` |

Usamos `WARN` para uma falha que permite nova tentativa, `ERROR` para o encerramento com
falha e `INFO` para o sucesso. Os registros acrescentados não incluem endereço, coordenadas,
e-mail, token, mensagem da exceção ou seu stack trace. A mensagem original de um cliente HTTP
pode conter uma URL com dados da requisição, por isso registramos apenas a classe da falha.

O identificador se refere à consulta de rota e não substitui um rastreamento de toda a
requisição HTTP. Os registros de sucesso exigem nível `INFO` para esse componente; falhas
continuam visíveis com o nível `WARN` usado nos testes de integração existentes.

## Validação e evidências

Adicionamos `MapsResilienciaTest`, com cinco testes automatizados:

1. Recuperação na segunda tentativa, preservando o resultado e correlacionando os registros.
2. Recuperação na terceira e última tentativa permitida.
3. Encerramento após três falhas, propagando a mesma exceção e sem expor dados sensíveis nos logs.
4. Propagação de erro não relacionado à comunicação, sem repetir a chamada.
5. Sucesso na primeira tentativa e identificadores diferentes para consultas independentes.

Os testes verificam a quantidade de chamadas, o valor de `Rota` e os registros capturados
com `OutputCaptureExtension`. Nenhum teste existente foi alterado.

Executamos `mvn --batch-mode --no-transfer-progress clean verify` em 11/11/2026, com Maven
3.9.15 e JDK 27.0.1.0.3, mantendo a compilação com `release 26` definida no projeto.
O workflow de CI continua configurado para Java 26.

```text
Testes unitários:     34 executados, 0 falhas, 0 erros, 0 ignorados
Testes de integração: 11 executados, 0 falhas, 0 erros, 0 ignorados
Total:               45 testes aprovados
JaCoCo:              718 de 733 linhas cobertas (97,95%)
All coverage checks have been met.
BUILD SUCCESS
Total time: 02:49 min
Finished at: 2026-11-11T12:23:21-03:00
```

Trecho da recuperação na segunda tentativa, extraído da execução:

```text
12:21:50.984 [main] WARN br.edu.foodnow.integration.MapsLocalizacaoAdapter -- operacao=calcular_rota operacaoId=c1144c2a-b0c6-442c-8e6c-163fce276c48 tentativa=1 limite=3 tipoFalha=ResourceAccessException resultado=nova_tentativa
12:21:50.996 [main] INFO br.edu.foodnow.integration.MapsLocalizacaoAdapter -- operacao=calcular_rota operacaoId=c1144c2a-b0c6-442c-8e6c-163fce276c48 tentativa=2 limite=3 resultado=sucesso
```

Trecho do encerramento por indisponibilidade persistente:

```text
12:21:51.292 [main] WARN br.edu.foodnow.integration.MapsLocalizacaoAdapter -- operacao=calcular_rota operacaoId=94159f40-a26a-495a-a5d7-bad8df950078 tentativa=1 limite=3 tipoFalha=ResourceAccessException resultado=nova_tentativa
12:21:51.302 [main] WARN br.edu.foodnow.integration.MapsLocalizacaoAdapter -- operacao=calcular_rota operacaoId=94159f40-a26a-495a-a5d7-bad8df950078 tentativa=2 limite=3 tipoFalha=ResourceAccessException resultado=nova_tentativa
12:21:51.311 [main] ERROR br.edu.foodnow.integration.MapsLocalizacaoAdapter -- operacao=calcular_rota operacaoId=94159f40-a26a-495a-a5d7-bad8df950078 tentativa=3 limite=3 tipoFalha=ResourceAccessException resultado=tentativas_esgotadas
```

As mensagens de erro acima são esperadas nos testes que simulam falhas. Os testes verificam
o encerramento e a quantidade de chamadas, além dos registros gerados.

Os trechos da saída do Maven estão em [Evidência da verificação](evidencias/verificacao.txt),
e os registros dos cinco cenários estão em [Evidência de diagnóstico](evidencias/diagnostico.log).
O relatório completo de cobertura é gerado em `target/site/jacoco/index.html`.

## Limitações

As tentativas são imediatas e limitadas. Isso cobre uma falha transitória que desaparece
entre chamadas, mas pode aumentar a carga sobre um provedor indisponível. Não implementamos
backoff, jitter ou circuit breaker nesta atividade. Uma integração real precisaria avaliar
esses mecanismos conforme sua latência, volume e política de uso.

Limitar o número de tentativas não limita o tempo de uma chamada bloqueada. O cliente HTTP
real também precisaria de timeouts de conexão e leitura. Não criamos uma rota aproximada
como fallback: sem resposta válida, a operação continua falhando em vez de calcular uma
cotação com dados inventados.

## Reflexão

A solução melhora **a resistência à falha e o diagnóstico**. A repetição limitada permite
concluir a consulta quando o provedor se recupera entre as chamadas. Os registros mostram
qual consulta falhou, o tipo da falha, quantas tentativas ocorreram e se houve recuperação
ou encerramento. Em uma indisponibilidade persistente, a operação ainda falha, mas o problema
fica identificado e a aplicação não continua repetindo indefinidamente.
