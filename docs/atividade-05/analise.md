# Atividade 05 — 2º bimestre: Resiliência e Observabilidade

Integrante: Pedro Terra (`PedroTerra11`).

## Fluxo escolhido

O fluxo escolhido é o envio da notificação de pagamento, coordenado por `PagamentoService.processar` e executado por `NotificacaoService.notificarPagamento`, utilizando o cliente simulado `FakeEmailClient`.

O comportamento esperado é registrar o resultado do pagamento, atualizar o pedido e, quando aprovado, criar a entrega. A notificação informa o cliente sobre o resultado, mas sua disponibilidade não deve impedir a conclusão desse processamento.

A implementação parte da base refatorada do professor, commit `c8eab129588411c8abe0b87cfc21a6ed4e1b3d68`. Esta branch contém somente a Atividade 05 do segundo bimestre, independente dos PRs das atividades 01, 02 e 03.

## Falha e impacto

Na implementação anterior, `PagamentoService` chamava a notificação dentro do método transacional, depois dos saves de pedido/pagamento e antes da criação da entrega. Esses saves ainda não garantiam o commit. Uma exceção não tratada do cliente de e-mail poderia provocar rollback dos registros locais, embora o gateway já tivesse processado o pagamento. O envio também não produzia registros específicos de diagnóstico.

Foram simulados dois momentos da mesma falha de comunicação:

- Falha antes de registrar o envio: o e-mail não é aceito pelo simulador.
- Falha após registrar o envio: o provedor aceita a mensagem, mas a confirmação não chega à aplicação. Repetir automaticamente poderia duplicar a notificação.

O simulador padrão não apresenta indisponibilidade espontânea. Os testes usam um spy que mantém o comportamento real nos cenários de sucesso e injeta a falha apenas na sobrecarga `enviar(Notificacao)` utilizada pelo pagamento.

## Melhoria localizada

Foi criada `EmailIndisponivelException` para indicar uma falha de comunicação cujo resultado de envio pode ser incerto. `NotificacaoService` trata essa exceção somente ao enviar a notificação de pagamento. O tipo de falha fica documentado no cliente simulado; um adaptador real deve traduzir a falha de transporte para esse tipo.

O tratamento permite retornar ao fluxo normal de `PagamentoService`, que continua criando a entrega quando necessário e finalizando sua transação. Não são capturadas todas as `RuntimeException`: falhas inesperadas de programação e erros no cálculo da rota continuam seguindo o comportamento anterior.

Não há retry automático. A escolha evita uma tentativa adicional quando a mensagem já pode ter sido aceita. Também não há garantia de entrega, fila persistente ou implementação geral de idempotência. Essa solução contém o impacto da falha secundária; não recupera a notificação perdida.

A alteração fica restrita ao envio de pagamento. Confirmação de pedido e início de entrega mantêm o comportamento anterior. Não foram alterados endpoints, DTOs, regras de valores, resultado do gateway ou fluxo de sucesso.

## Registros de diagnóstico

Os registros identificam a operação `notificarPagamento`, o pedido, o status do pagamento e o número de tentativas. No sucesso, o resultado é `ENVIADA`. Na falha, o nível é WARN e são registrados o tipo da exceção, `ENVIO_INCERTO` e o tratamento `CONTINUAR_SEM_REENVIO`.

O termo `ENVIO_INCERTO` foi escolhido porque uma exceção de comunicação não permite afirmar se o provedor recebeu a mensagem. O log registra o tratamento do envio, sem afirmar que o commit do pagamento já ocorreu; erros posteriores ainda podem impedir a conclusão da transação.

Não são registrados destinatário, corpo do e-mail, token, coordenadas, mensagem da exceção ou stack trace contendo dados potencialmente sensíveis. A correlação pelo identificador do pedido permite investigar a operação sem expor esses conteúdos.

## Testes

`NotificacaoPagamentoResilienciaIT` testa as combinações de pagamento aprovado/rejeitado com sucesso, falha anterior ao envio e falha após aceitação: seis casos ao todo.

Cada caso verifica HTTP 201, consulta posterior do pagamento e pedido, presença de entrega apenas quando aprovado, uma única chamada ao envio de pagamento e quantidade de notificações registradas. No cenário de aceitação seguida de falha, permanece exatamente uma notificação, demonstrando que não ocorre reenvio pela política adotada.

Os logs são capturados e verificados pelo teste: identificador correto, status, resultado e tratamento. Um marcador sensível é colocado propositalmente na mensagem da exceção, e o teste garante que ele, o token, o destinatário e um trecho do conteúdo do e-mail não aparecem nos registros.

Os testes de API existentes e as referências geográficas não foram modificados. Os resultados da suíte e trechos reais dos logs estão em [validacao.md](validacao.md).

## Benefício, custo e limitações

A melhoria reduz o impacto de uma falha de notificação sobre a operação de pagamento e entrega, além de permitir identificar o pedido afetado e o tratamento adotado. O custo é aceitar que uma notificação pode não chegar ao cliente, exigindo investigação operacional. Uma implementação futura que precise de entrega garantida exigiria persistência da intenção de envio, reprocessamento e uma estratégia contra duplicidade.

Esta solução não limita o tempo de uma chamada que nunca retorna e não cobre todos os pontos de falha do fluxo. Também não elimina inconsistências decorrentes de outros erros posteriores à autorização do gateway. As integrações continuam locais e simuladas, sem infraestrutura adicional.

## Questão para reflexão

A solução melhora ambos os aspectos. O sistema fica mais resistente porque uma falha conhecida no envio do e-mail deixa de interromper o pagamento e a criação da entrega. Também fica mais fácil de diagnosticar, pois os registros mostram a operação, o pedido, o tipo de falha e a decisão de continuar sem reenviar. Isso não equivale a garantir a entrega da notificação: o resultado incerto permanece explicitamente registrado.
