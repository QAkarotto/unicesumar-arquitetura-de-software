# Atividade 04 — Evento assíncrono

## Operação e modelagem

A operação selecionada é confirmar um pedido existente. A operação principal termina com sucesso quando o pedido muda para `CONFIRMADO` e é salvo. O envio de e-mail de confirmação é a ação secundária: ele pode ocorrer separadamente e não deve condicionar a resposta da confirmação.

**Fluxo atual:** cliente → `PedidoController` → `PedidoService.confirmar` → salva o pedido e envia e-mail diretamente pelo `FakeEmailClient` → resposta HTTP. Assim, o serviço de pedidos conhece e invoca a integração de e-mail durante a operação principal.

**Fluxo proposto:** cliente → `PedidoController` → `PedidoService.confirmar` (produtor) → salva o pedido e publica `PedidoConfirmado` → commit → listener assíncrono (consumidor) → envia um único e-mail pelo `FakeEmailClient`. O evento é um fato já ocorrido e carrega `pedidoId`, `emailCliente`, `regiaoEntrega` e `estimativaEntregaMinutos`; o consumidor não precisa consultar entidades ou depender de dados ainda não confirmados.

O listener usa `@TransactionalEventListener(AFTER_COMMIT)` e `@Async`, habilitado por `@EnableAsync`. A falha de envio é capturada e registrada em log, sem propagar para a confirmação já concluída. Endpoints e contratos públicos foram preservados.

## Testes e execução

Foi adicionado teste unitário do consumidor para verificar os dados enviados e para simular exceção no e-mail, garantindo que ela não escape do consumidor. Os testes de fluxo de API aguardam a conclusão da notificação assíncrona antes de verificar o registro. Essa mudança de espera não altera as expectativas da API.

Resultado de `mvn clean verify`:

```text
[INFO] Unit tests: 30 run, 0 failures, 0 errors, 0 skipped
[INFO] Integration tests: 7 run, 0 failures, 0 errors, 0 skipped
[INFO] JaCoCo analyzed bundle 'FoodNow' with 74 classes
[INFO] JaCoCo analyzed bundle 'foodnow' with 73 classes
[INFO] All coverage checks have been met.
[INFO] BUILD SUCCESS
```

## Questões

### Qual dependência entre componentes foi reduzida?

`PedidoService` não depende mais de `FakeEmailClient` nem conhece o mecanismo de notificação. Ele depende do publicador de eventos e expressa apenas o fato de que o pedido foi confirmado. O consumidor concentra o efeito de enviar a confirmação por e-mail.

### O que pode acontecer se a aplicação encerrar depois da publicação e antes do processamento?

O evento existe apenas na memória do processo. Se a aplicação encerrar antes de o listener executá-lo, o e-mail pode não ser enviado e o evento não será recuperado após reinicialização. Eventos locais assíncronos não oferecem entrega durável, reprocessamento ou idempotência.

### Fila ou Publish/Subscribe ao evoluir para broker?

Uma fila seria mais adequada ao fluxo escolhido: existe uma única ação consumidora, enviar um e-mail, e cada confirmação deve ser processada por um dos trabalhadores disponíveis. Se surgirem consumidores independentes (por exemplo, e-mail, métricas e atualização de outro sistema), Publish/Subscribe permitiria que cada um recebesse sua própria cópia do evento.
