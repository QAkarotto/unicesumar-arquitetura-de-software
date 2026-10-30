# Atividade 03 - 2BIM: Integrações Externas e Tratamento de Falhas no FoodNow

## Objetivo

Analisar uma integração externa do **FoodNow** e implementar uma melhoria localizada para reduzir o impacto de falhas na comunicação entre sistemas.

## Contexto

O FoodNow possui integrações simuladas com serviços externos, como localização, pagamento e notificações. Essas dependências podem apresentar indisponibilidade, lentidão ou respostas inesperadas.

## Atividade

Escolha **uma integração externa** do projeto e:

1. identifique como o FoodNow depende atualmente desse serviço;
2. descreva pelo menos **dois cenários de falha** relevantes;
3. implemente uma melhoria para tratar um desses cenários, utilizando, quando aplicável:
   - timeout;
   - retry;
   - tratamento de erros;
   - idempotência;
4. crie ou ajuste testes para validar o comportamento implementado;
5. explique brevemente o benefício e o trade-off da decisão adotada.

Não é necessário modificar todas as integrações do projeto.

## Restrições

- Não altere os contratos existentes da API.
- Não altere os testes de API existentes para adequá-los à solução.
- Preserve o comportamento já existente nos cenários de sucesso.
- Mantenha a cobertura mínima configurada no projeto.

Valide a solução com:

```bash
mvn clean verify
```

## Entrega

A atividade pode ser realizada **individualmente ou em dupla**.

A entrega deverá ser feita por **Pull Request** contendo:

- integrantes;
- integração escolhida;
- cenários de falha analisados;
- decisão adotada;
- principais alterações;
- benefício e trade-off;
- resultado do `mvn clean verify`.
