# Atividade 03 — Integrações Externas e Tratamento de Falhas no FoodNow

## Integrante
Samuel Vitor da Cruz Silva

## Integração escolhida
Integração de mapas/localização, representada por `MapsLocalizacaoAdapter` e `FakeMapsClient`. O fluxo atual é `LocalizacaoService` → `ProvedorLocalizacao` → `MapsLocalizacaoAdapter` → `FakeMapsClient`. O adapter traduz as respostas do cliente simulado para os tipos utilizados pelo domínio (`Localizacao` e `Rota`). As operações são consultas de coordenadas e cálculo de rota.

## Cenários de falha analisados
1. **Indisponibilidade temporária ou timeout:** o provedor pode falhar antes de responder, interrompendo uma consulta de localização ou rota.
2. **Resposta inesperada:** o provedor pode retornar coordenadas ausentes ou em formato inválido, que não podem ser convertidas para os tipos numéricos esperados.

## Decisão adotada
Foi implementada uma política de retry limitada a três tentativas para exceções classificadas como `FalhaTemporariaMapsException`. Se o provedor continuar indisponível, a operação termina com uma mensagem explícita indicando o limite de tentativas. A política é aplicada tanto à busca de coordenadas quanto ao cálculo de rota.

Para resposta com coordenadas inválidas, o adapter informa o problema por meio de `IllegalStateException`, em vez de deixar escapar diretamente um erro de conversão. Esse caso não é repetido automaticamente, pois a chamada retornou uma resposta, mas o conteúdo não atende ao formato esperado.

Como o projeto usa um cliente de mapas simulado, os testes injetam falhas temporárias para exercitar o retry. Em uma integração real, o cliente de infraestrutura deve converter falhas transitórias de transporte — como timeout ou conexão interrompida — para `FalhaTemporariaMapsException`.

## Principais alterações
- Inclusão de `FalhaTemporariaMapsException` para identificar falhas transitórias do provedor.
- Retry com limite de três tentativas no `MapsLocalizacaoAdapter`.
- Mensagem controlada para resposta de coordenadas inválidas.
- Testes para recuperação após falhas temporárias, esgotamento das tentativas e resposta inválida.
- Preservação do comportamento dos casos de sucesso existentes e dos contratos da API; os testes de API não foram alterados.

## Benefício e trade-off
O retry permite recuperar consultas quando uma falha temporária desaparece rapidamente, reduzindo interrupções causadas por instabilidade momentânea. O limite de três tentativas evita repetição infinita. Como trade-off, uma consulta pode demorar mais e gerar até três chamadas ao provedor; a estratégia não resolve uma indisponibilidade persistente. O retry é apropriado para essas operações de leitura, pois repetir uma consulta de coordenadas ou rota não cria uma nova transação de negócio.

## Validação
O comando `mvn clean verify` foi executado no projeto atualizado em 30/10/2026 e terminou com `BUILD SUCCESS`.

- Testes unitários: 32 executados, sem falhas ou erros.
- Testes de integração: 11 executados, sem falhas ou erros.
- JaCoCo: todas as verificações de cobertura configuradas foram atendidas.

Durante os testes de integração, o Mockito exibiu avisos relacionados ao carregamento do `byte-buddy-agent`, mas os testes foram concluídos sem falhas ou erros e o Maven finalizou com `BUILD SUCCESS`.
