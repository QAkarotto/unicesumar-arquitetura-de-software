# Atividade 03 — tolerância a falhas nas notificações

## Integrantes

- Felipe de Oliveira Guimarães
- Otavio Alves Brisky
- Turma: ESOFT8S

## Integração escolhida e diagnóstico

Integração de envio de e-mails, simulada localmente por
`src/main/java/br/edu/foodnow/integration/FakeEmailClient.java`.
Não há comunicação com um servidor SMTP ou serviço externo real. O cliente
padrão sempre registra os envios em uma `CopyOnWriteArrayList` em memória;
`getEnviadas()` devolve uma cópia e `limpar()` remove os registros.

A aplicação depende dessa integração por meio de
`service/NotificacaoService.java`, sob `src/main/java/br/edu/foodnow/`:

| Chamador | Método de notificação | Dependências e comportamento anterior |
|---|---|---|
| `PedidoService.confirmar` | `notificarConfirmacao` | Usa `LogisticaService.resumirPedido` para compor região e estimativa; envia após confirmar e antes de salvar o pedido |
| `PagamentoService.processar` | `notificarPagamento` | Usa `LocalizacaoService.calcularRota` e `Notificacao.adicionarReferenciaGeografica`; envia após salvar pedido e pagamento e antes de criar a entrega quando aprovado |
| `EntregaService.atualizarStatus` | `notificarInicioEntrega` | Envia ao mudar a entrega para `EM_ROTA` e o pedido para `EM_ENTREGA`, antes dos saves finais |

`Notificacao` contém destinatário, assunto e mensagem. Confirmação e início
de entrega usavam a sobrecarga `enviar(String, String, String)`; pagamento
usava `enviar(Notificacao)`. Ambas apenas armazenavam uma notificação.

O envio é síncrono, dentro de métodos de aplicação com `@Transactional`.
Antes, não havia contenção de falhas do e-mail: uma exceção não verificada
propagada pelo cliente poderia interromper a operação e provocar rollback.
Isso acoplava a disponibilidade das operações à integração de notificação,
embora as regras de confirmação, pagamento e entrega não validem recebimento
de e-mail. O simulador original não produzia essa falha espontaneamente;
os novos testes a reproduzem com clientes substitutos controlados.

## Cenários de falha

| Cenário | Impacto potencial | Tratamento e evidência |
|---|---|---|
| Provedor indisponível antes de aceitar a mensagem | Falha no e-mail poderia interromper confirmação, pagamento ou início da entrega | `EmailIndisponivelException` é contida no envio, gera aviso e permite continuar; testes unitários cobrem as quatro mensagens e testes HTTP verificam operações persistidas |
| Provedor aceita a mensagem, mas a resposta é perdida | Resultado incerto; repetir o envio pode duplicar o e-mail | Uma única tentativa, sem retry; teste registra a mensagem antes de lançar a exceção e verifica exatamente um registro e uma chamada |
| Provedor demora sem devolver resposta ou lançar exceção | A requisição continua ocupada e a transação pode permanecer aberta | Limitação não resolvida: o cliente atual é local e não oferece transporte ou configuração de timeout; um adaptador real deve impor prazos de conexão/leitura |

O segundo cenário é uma simulação de resultado incerto, não uma implementação
de timeout de rede. Não é possível afirmar se houve entrega efetiva ao destinatário.

## Decisão e implementação localizada

O serviço passou a tratar envio de e-mail com melhor esforço: uma falha de
disponibilidade conhecida não desfaz a operação principal.

1. Criada `integration/EmailIndisponivelException.java`, que representa uma
   falha operacional de disponibilidade do e-mail. O cliente padrão mantém
   seu comportamento de sucesso; os clientes de teste lançam essa exceção.
2. Documentado esse contrato no método `FakeEmailClient.enviar(Notificacao)`.
3. Adicionado `NotificacaoService.enviarComTolerancia`, que envolve apenas
   a chamada ao cliente em `try/catch`. Todas as mensagens passam por esse
   método, mantendo destinatário, assunto, conteúdo e referência geográfica.
4. O catch captura exclusivamente `EmailIndisponivelException`. Não captura
   `RuntimeException` genericamente; defeitos inesperados permanecem visíveis.
   As consultas de localização e a composição das mensagens ficam fora do catch.
5. Na falha, registra um aviso com o assunto fixo da notificação, sem incluir
   destinatário, conteúdo ou a mensagem da exceção. Não registra envio como
   bem-sucedido nem cria novo status de pedido, pagamento ou entrega.

Não há retry, espera, fila, mudança de transação ou nova dependência. A escolha
de uma tentativa reduz risco de duplicação e mantém o escopo localizado.
Um futuro adaptador real precisará traduzir suas falhas de disponibilidade
para a exceção específica; outras exceções não são absorvidas automaticamente.

## Benefício, trade-off e limites

Benefício: indisponibilidade conhecida do e-mail deixa de impedir operações
válidas da aplicação. Os novos testes HTTP mostram confirmação persistida,
pagamento aprovado com criação de entrega, pagamento rejeitado sem entrega e
início de entrega persistido, mesmo quando nenhum envio é registrado.

Trade-off: uma notificação pode não chegar e não será reenviada. Não há fila
durável, reprocessamento, garantia de entrega ou garantia de exatamente uma
entrega. Uma falha após aceitação permanece ambígua. Logs permitem observar
a ocorrência, mas não recuperam a mensagem.

O envio permanece síncrono e pode ocorrer antes do commit da transação. Assim,
uma falha posterior de outra dependência ainda pode desfazer a operação após
o envio de um e-mail. Resolver isso exigiria um mecanismo como outbox e está
fora desta melhoria. Falhas de mapas, pagamento e despacho também não foram
absorvidas por esse tratamento.

## Contratos e escopo preservados

Não foram alterados controllers, DTOs, OpenAPI, rotas, corpos ou códigos HTTP
declarados. Não foram modificadas validações, cálculos, transições de estado
ou regras de pedidos, pagamentos e entregas. Na falha de e-mail tratada, as
operações passam a concluir com seus códigos e representações habituais,
em vez de propagar a exceção do cliente.

Os testes de API anteriores e as referências JSON/textuais de geografia foram
mantidos integralmente. Os novos testes são adicionais. O `pom.xml`, o pipeline,
as configurações e as exclusões/limite de cobertura não foram modificados.

## Testes e resultados reais

Execução concluída em **29/10/2026**, na branch `8sfelipe-atividade03-2bim`.

Comando PowerShell:

```powershell
mvn --batch-mode --no-transfer-progress "-Dmaven.repo.local=$env:USERPROFILE\.m2\repository" clean verify
```

O parâmetro apenas seleciona o cache de dependências do usuário.

| Grupo | Execuções | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| Testes unitários — Surefire | 39 | 0 | 0 | 0 |
| Testes de API — Failsafe | 13 | 0 | 0 | 0 |
| Total | **52** | **0** | **0** | **0** |

Novos testes, sob `src/test/java/br/edu/foodnow/`:

- `service/NotificacaoServiceTest.java`: 10 execuções. Quatro casos de sucesso,
  quatro de indisponibilidade antes do envio, um de exceção inesperada propagada
  e um de falha após aceitação sem retry ou duplicação.
- `controller/NotificacaoResilienciaIT.java`: duas execuções, com pagamento
  aprovado e rejeitado. Usa banco H2 separado e substitui o cliente apenas no
  contexto desse teste. Verifica por GET os estados persistidos, a existência
  ou ausência da entrega, os códigos HTTP e a quantidade exata de tentativas.

Os 40 testes anteriores da branch também passaram, incluindo o fluxo completo,
os erros existentes e a comparação integral das referências de geografia.

Saída final do build:

```text
All coverage checks have been met.
BUILD SUCCESS
```

Cobertura de linhas extraída de `target/site/jacoco/jacoco.xml`:

| Escopo | Cobertas | Não cobertas | Cobertura |
|---|---:|---:|---:|
| Relatório geral | 709 | 15 | **97,93%** |
| Check com a exclusão de `FoodNowApplication` já existente | 708 | 13 | **98,20%** |

O `jacoco:check` aprovou o mínimo de **80%**, sem novas exclusões.
O relatório HTML é gerado em `target/site/jacoco/index.html`; os resultados
individuais ficam em `target/surefire-reports` e `target/failsafe-reports`.
