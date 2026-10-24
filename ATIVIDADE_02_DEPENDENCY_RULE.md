# Atividade 02 — Dependency Rule e Limites Arquiteturais

## Integrante

Samuel Vitor da Cruz Silva

## Dependência escolhida

Envio de notificações por e-mail.

## Análise do problema

Antes da refatoração, `NotificacaoService` recebia diretamente `FakeEmailClient`. Isso fazia a regra da aplicação conhecer a classe concreta de integração. Se o mecanismo de envio fosse trocado, a classe de serviço precisaria ser alterada e os testes ficariam ligados ao cliente simulado usado pela aplicação.

## Decisão adotada

Foi criado o contrato `br.edu.foodnow.notificacao.EnvioNotificacao`. `NotificacaoService` passa a depender dessa abstração, enquanto `FakeEmailClient` a implementa. O cliente simulado continua sendo um componente Spring e mantém seus métodos de envio, armazenamento em memória e limpeza, preservando os usos atuais nos testes de API.

## Principais alterações

- Criada a interface `EnvioNotificacao` com operações de envio de mensagem e de objeto `Notificacao`.
- Alterado `FakeEmailClient` para implementar o contrato.
- Alterado `NotificacaoService` para receber e utilizar `EnvioNotificacao`, sem importar `FakeEmailClient`.
- Adicionado `NotificacaoServiceTest`, que usa uma implementação falsa do contrato para verificar o destinatário, o assunto e o conteúdo da mensagem sem instanciar a integração concreta e sem depender do Mockito nesse teste.
- Nenhum endpoint ou teste de API foi alterado.

## Direção das dependências

Antes: `NotificacaoService → FakeEmailClient`.

Depois: `NotificacaoService → EnvioNotificacao ← FakeEmailClient`.

A regra da aplicação depende agora do contrato; a integração concreta depende do mesmo contrato. Isso permite substituir a implementação de envio com menor impacto e testar o serviço com uma implementação falsa do contrato, sem precisar utilizar o cliente concreto.

## Testes e validação

O comando `mvn clean verify` foi executado no ambiente local do projeto, com Oracle JDK 26.0.2.1, em Windows 11, e terminou com `BUILD SUCCESS`.

- Testes unitários: 30 executados, 0 falhas e 0 erros.
- Testes de integração: 11 executados, 0 falhas e 0 erros.
- Verificação de cobertura pelo JaCoCo: aprovada.

Durante os testes de integração, o console exibiu avisos relacionados ao carregamento de `byte-buddy-agent` pelo Mockito. Apesar desses avisos, a suíte de testes foi concluída sem falhas ou erros e o Maven confirmou `BUILD SUCCESS`.
