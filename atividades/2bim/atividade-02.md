## Atividade 02 – Dependency Rule e Limites Arquiteturais

**Integrante:** Ramon Albini Vieira e Eduardo Henrique

### Dependência escolhida

Integração de notificações por e-mail.

### Problema identificado

Originalmente, `NotificacaoService` dependia diretamente de `FakeEmailClient`, uma implementação localizada no pacote de infraestrutura.

Essa dependência fazia com que a regra responsável por compor e enviar notificações conhecesse uma implementação específica de envio de e-mails.

### Decisão arquitetural

Foi criado o contrato `CanalNotificacao`, no pacote `br.edu.foodnow.notificacao`.

O `NotificacaoService` passou a depender dessa abstração, enquanto `EmailNotificacaoAdapter`, localizado no pacote de integração, implementa o contrato e delega o envio ao `FakeEmailClient`.

### Principais alterações

- Criada a interface `CanalNotificacao`.
- Criado o adaptador `EmailNotificacaoAdapter`.
- Modificado `NotificacaoService` para depender da abstração.
- Criados testes unitários do serviço utilizando uma implementação em memória.
- Criado teste para verificar o encaminhamento das notificações pelo adaptador.

### Comportamento preservado

As mensagens, destinatários, assuntos e regras de notificação foram mantidos.

Não foram alterados endpoints, contratos REST, regras de negócio ou integrações simuladas.

Os testes de API existentes foram preservados.

### Direção das dependências

Após a refatoração, `NotificacaoService` depende de um contrato da aplicação (`CanalNotificacao`), e `EmailNotificacaoAdapter` implementa esse contrato.

Isso permite substituir a implementação de envio de notificações com menor impacto sobre as regras da aplicação.

### Validação

- Comando: `mvn clean verify`
- Cobertura JaCoCo: A cobertura de linhas informada pelo relatório JaCoCo foi de 97,78%, acima do mínimo exigido de 80%.