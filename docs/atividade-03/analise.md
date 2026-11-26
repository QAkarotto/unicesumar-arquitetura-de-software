# Atividade 03 — 2º bimestre: Integrações Externas e Tratamento de Falhas

Integrante: Pedro Terra (`PedroTerra11`).

## Integração escolhida

Foi escolhida a integração de mapas do FoodNow, formada por `FakeMapsClient` e `MapsLocalizacaoAdapter`. O adaptador implementa `ProvedorLocalizacao` e traduz as coordenadas e rotas do simulador para os tipos da aplicação. `LocalizacaoService` e `LogisticaService` utilizam essa abstração para consultas e cálculos que participam dos fluxos de atendimento, pedido, pagamento, notificação e entrega.

A implementação utiliza a base refatorada do professor, commit `c8eab129588411c8abe0b87cfc21a6ed4e1b3d68`. A branch contém somente a Atividade 03 do segundo bimestre, sem incorporar os PRs das atividades anteriores.

O cliente padrão é local e determinístico. Ele não realiza chamadas de rede nem falha espontaneamente. As falhas foram reproduzidas nos testes com um mock controlado do cliente; não foi necessário instalar um serviço externo ou alterar os cenários normais do simulador.

## Comportamento anterior e cenários de falha

Antes, o adaptador fazia uma única chamada ao cliente. Qualquer exceção interrompia a consulta, mesmo quando uma segunda tentativa poderia funcionar.

| Cenário | Impacto | Decisão |
| --- | --- | --- |
| Indisponibilidade temporária antes da resposta | A consulta falha e pode interromper o fluxo que depende da rota ou localização | Repetir somente a falha classificada como transitória, até três tentativas no total |
| Indisponibilidade persistente | Repetir indefinidamente bloquearia a operação e aumentaria a carga no provedor | Encerrar após a terceira falha e propagar a última exceção |
| Resposta com latitude em formato inválido | A conversão numérica falha; repetir uma resposta inválida não garante recuperação | Preservar a falha de conversão e não repetir |
| Provedor lento sem retornar nem lançar exceção | A requisição pode ficar ocupada aguardando a consulta | Limitação não resolvida por retry; um transporte real deve configurar timeout |

## Melhoria implementada

Foi acrescentada `MapsIndisponivelException`, representando uma falha transitória de comunicação passível de repetição. O contrato dessa falha está documentado nos métodos do cliente simulado. Um cliente real deve classificar as falhas do seu transporte antes de sinalizá-las dessa forma; erros de programação e entrada inválida não devem ser convertidos indiscriminadamente nessa exceção.

`MapsLocalizacaoAdapter` aplica a mesma política nas consultas de coordenadas e de rota:

1. Executar a consulta.
2. Retornar imediatamente se houver sucesso.
3. Em `MapsIndisponivelException`, repetir enquanto restarem tentativas.
4. Na terceira falha, propagar a última exceção, preservando sua identidade e mensagem.

O limite é de **três chamadas, incluindo a primeira**, e não três repetições adicionais. As conversões de resposta ficam fora da repetição, de forma que uma latitude malformada não provoca novas chamadas. Exceções de outros tipos continuam sendo propagadas sem retry.

A política foi colocada no adaptador para manter a decisão de comunicação com o provedor na fronteira de integração. `LocalizacaoService`, `LogisticaService` e os demais serviços não precisam conhecer o mecanismo. Não foram criados endpoints ou novas respostas HTTP, nem foram alterados DTOs ou tratamentos do controller. Ao esgotar as tentativas, a falha continua propagando para o tratamento já existente na aplicação.

## Benefício e trade-off

O benefício é recuperar consultas após uma indisponibilidade breve, sem exigir que o consumidor repita toda a operação. No sucesso sem falhas continua existindo apenas uma chamada, com os mesmos resultados de distância, prazo e região.

As duas operações são consultas de mapas, sem criação de pedido, cobrança ou envio de mensagem no provedor. Repeti-las não introduz efeitos de negócio duplicados nessa integração. Isso não torna o fluxo inteiro de pedidos idempotente e não autoriza aplicar a mesma repetição automaticamente a pagamentos ou notificações.

O custo é fazer até duas chamadas adicionais e aumentar o tempo de resposta quando há falha. O limite se aplica a cada consulta do adaptador; um fluxo que realiza várias consultas pode acumular mais chamadas. A solução não resolve indisponibilidade prolongada e não inventa uma rota alternativa que possa mudar valores ou regras de atendimento.

Como o provedor desta atividade é simulado e local, as tentativas são imediatas, sem espera artificial. Em uma integração remota, essa política precisaria considerar backoff, jitter, limites do provedor e timeouts de conexão e leitura. O número de tentativas não limita a duração de uma chamada que nunca retorna. Essas medidas não foram acrescentadas para manter a alteração localizada no cenário demonstrado.

## Testes

`MapsRepeticaoTest` reproduz:

- sucesso imediato e recuperação após uma ou duas falhas, nas duas operações;
- indisponibilidade persistente, com exatamente três chamadas;
- propagação da última exceção quando as três falhas são diferentes;
- erro não transitório, com uma única chamada;
- coordenadas malformadas, sem repetição da consulta.

Os testes conferem a quantidade de chamadas e o conteúdo de rota/coordenadas retornado. Os testes existentes de `MapsLocalizacaoAdapterTest`, das integrações e da API foram preservados, incluindo as referências integrais de `GeografiaContratoIT`.

Os resultados de `mvn clean verify` e a cobertura estão em [validacao.md](validacao.md).
