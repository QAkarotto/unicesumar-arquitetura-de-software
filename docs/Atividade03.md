# Atividade 03 — Resiliência do serviço de localização

## Dependência atual

O `FakeMapsClient` simula o serviço externo de mapas e oferece geocodificação (`buscarCoordenadas`) e cálculo de rotas (`calcularRota`). O `LocalizacaoService` usa esses métodos para converter coordenadas e centralizar distância, tempo e zona da rota. Esses dados participam do cadastro de endereços de restaurantes e clientes, do atendimento, da criação e cobrança de pedidos, da entrega e da simulação de entrega.

Antes desta atividade, `RestauranteService.simularEntrega` chamava `FakeMapsClient.calcularRota` diretamente, enquanto os outros fluxos passavam por `LocalizacaoService`. `PedidoService` também tinha um import de `FakeMapsClient` sem uso. A chamada direta foi removida e a simulação passou a usar `LocalizacaoService.avaliarRota`.

## Cenários de falha considerados

1. **Falha temporária ao consultar o provedor:** timeout, indisponibilidade momentânea ou erro temporário de servidor interrompe a busca de coordenadas ou rota. Sem tratamento, uma falha transitória encerra a operação mesmo que uma consulta repetida pudesse funcionar.
2. **Resposta inválida ou incompatível:** coordenadas ausentes ou malformadas, ou uma resposta que não pode ser convertida para os tipos esperados, pode interromper a operação. Repetir a mesma resposta não corrige um problema de formato; essa situação requer validação e tratamento específico no adaptador.

O cliente atualmente é uma simulação em memória, determinística e sem chamada de rede. Por isso não há timeout de conexão/leitura configurável neste ponto. Quando houver um cliente HTTP real, ele deverá impor esses limites de tempo e converter falhas transitórias (inclusive timeout) para `FalhaTemporariaMapasException`.

## Melhoria aplicada: retry limitado para falha temporária

`LocalizacaoService` agora tenta novamente operações de geocodificação e rota que lançam `FalhaTemporariaMapasException`. São feitas no máximo três tentativas, com espera crescente de 50 ms e 100 ms entre elas. Se a falha persistir, o serviço lança `ServicoLocalizacaoIndisponivelException`; `ApiExceptionHandler` converte essa falha em HTTP 503 usando o formato existente `ErroResponse`.

O retry não captura qualquer exceção: erros de programação e respostas inválidas não são tratados como indisponibilidade transitória e não são repetidos. As consultas de coordenadas e rotas são leituras sem efeito colateral, portanto repetir a mesma solicitação é idempotente em relação ao estado do FoodNow. A simulação continua entregando os mesmos valores nos cenários de sucesso.

## Testes e trade-off

Foram adicionados testes unitários para sucesso após uma falha temporária, esgotamento das três tentativas e interrupção da thread durante a espera. Também há teste unitário para o mapeamento do erro esgotado para HTTP 503. Os testes de API existentes não foram modificados.

O benefício é tolerar falhas breves e apresentar indisponibilidade explícita após um limite finito. O custo é latência adicional de até 150 ms antes da resposta de falha, além das chamadas repetidas ao provedor. O timeout real continua sendo responsabilidade do adaptador de rede quando a simulação for substituída por uma integração HTTP.
