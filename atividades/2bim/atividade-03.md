# Atividade 03 — 2BIM: Integrações Externas e Tratamento de Falhas no FoodNow

## 1. Integrante

**Nome:** Guilherme Augusto Eidam

**Modalidade:** Individual

## 2. Integração escolhida

A integração escolhida foi o envio de notificações por e-mail, realizado pelo serviço `NotificacaoService`, utilizando o cliente `FakeEmailClient`.

Na implementação original, o serviço chamava diretamente o cliente de e-mail. O cliente simulado armazenava as notificações em memória, sem implementar um mecanismo próprio para recuperação de falhas.

Essa dependência representa um possível ponto de falha: em uma integração real, o serviço de e-mail pode apresentar indisponibilidade ou erros durante o envio.

## 3. Cenários de falha analisados

### Cenário 1 — Falha temporária no envio

O serviço de e-mail pode apresentar uma falha momentânea durante uma tentativa de envio. Sem novas tentativas, a notificação poderia não ser enviada, mesmo que o serviço voltasse a funcionar logo depois.

**Tratamento implementado:** o FoodNow realiza novas tentativas, até o limite de três tentativas no total. Caso o envio seja concluído com sucesso, as tentativas restantes não são executadas.

### Cenário 2 — Falha persistente no envio

O serviço de e-mail pode continuar indisponível durante todas as tentativas, impedindo o envio da notificação.

**Tratamento implementado:** após três tentativas sem sucesso, o serviço registra o erro no log e não propaga a exceção de envio ao fluxo principal. A notificação, entretanto, pode não ser entregue.

## 4. Decisão arquitetural

Foi implementado um mecanismo localizado de novas tentativas (*retry*) no serviço `NotificacaoService`.

A solução utiliza uma constante para definir o limite de três tentativas e um método privado responsável por executar o envio, tratar exceções de execução e registrar as falhas.

O mecanismo foi aplicado aos métodos de notificação de confirmação do pedido, início da entrega e resultado do pagamento.

A implementação preserva os métodos públicos existentes, os assuntos e as mensagens das notificações e não modifica os contratos da API.

## 5. Testes implementados

Foi criada a classe `NotificacaoServiceRetryTest`, utilizando JUnit e Mockito para simular falhas no cliente de e-mail.

Foram validados dois comportamentos:

- **Falha temporária:** a primeira tentativa falha, a segunda é bem-sucedida e o cliente é chamado duas vezes.
- **Falha persistente:** todas as três tentativas falham, a exceção não interrompe o fluxo principal e o cliente é chamado três vezes.

Os testes de API existentes foram preservados.

## 6. Principais alterações

- `src/main/java/br/edu/foodnow/service/NotificacaoService.java`: implementação do mecanismo de novas tentativas e registro de falhas.
- `src/test/java/br/edu/foodnow/service/NotificacaoServiceRetryTest.java`: testes dos cenários de falha temporária e persistente.
- `atividades/2bim/atividade-03.md`: documentação da atividade, da decisão arquitetural e dos resultados da validação.

## 7. Benefícios e trade-offs

O principal benefício é aumentar a tolerância a falhas temporárias, possibilitando que uma notificação seja enviada mesmo quando a primeira tentativa falha.

Como trade-offs, as novas tentativas aumentam o número de chamadas ao serviço e podem produzir registros adicionais de erro. Em uma integração real, também existe o risco de envio duplicado se o provedor aceitar uma mensagem, mas a confirmação da operação se perder.

A solução não implementa idempotência, espera entre tentativas nem uma fila persistente. Portanto, ela não garante a entrega quando todas as tentativas falham.

## 8. Validação

Foi executado o comando:

`mvn clean verify`

**Resultado:** `BUILD SUCCESS`.

- Testes unitários: 31 executados, sem falhas ou erros.
- Testes de integração: 11 executados, sem falhas ou erros.
- Cobertura: todos os limites configurados no JaCoCo foram atendidos.

A validação foi concluída em 29 de outubro de 2026.

## 9. Conclusão

A implementação adiciona um mecanismo simples de recuperação de falhas no envio de notificações do FoodNow. Os testes demonstram o comportamento esperado diante de falhas temporárias e persistentes, enquanto os contratos existentes da API e os testes de integração são mantidos.