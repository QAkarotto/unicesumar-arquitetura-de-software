# Atividade 01 - 2BIM: APIs como Fronteiras Arquiteturais

## Objetivo

Evoluir uma API REST do FoodNow, analisando contratos, DTOs, compatibilidade e versionamento, com especificação OpenAPI e testes automatizados.

## Contexto

Na atividade anterior, a implementação interna foi refatorada sem alterar o contrato externo. Nesta atividade, o FoodNow deverá permitir que o cliente informe uma observação ao criar um pedido, como “Não incluir talheres”.

A observação será opcional, terá até 500 caracteres e deverá ser armazenada e disponibilizada nas respostas que representam o pedido. Não haverá edição posterior da observação. Pedidos enviados sem essa informação deverão continuar funcionando.

## Atividade

1. Analise o contrato das quatro operações de pedidos: `POST /pedidos`, `GET /pedidos/{id}`, `POST /pedidos/{id}/itens` e `POST /pedidos/{id}/confirmar`. Identifique métodos, parâmetros, corpos, campos, validações, respostas e status HTTP. Relacione os DTOs e os testes existentes.

2. Antes de implementar, represente o contrato atual dessas operações em uma especificação OpenAPI, em YAML ou JSON. O projeto não possui essa especificação. Inclua schemas, parâmetros, request bodies quando aplicáveis, respostas de sucesso e erro e pelo menos um exemplo de request ou response.

3. Proponha o contrato da evolução. Defina o campo e os DTOs afetados, o tratamento de ausência, `null`, texto vazio e limite excedido. Identifique consumidores potencialmente afetados, distinguindo os encontrados no repositório dos hipotéticos. Classifique a mudança como compatível ou incompatível, explicite as condições dessa avaliação e justifique a necessidade ou não de uma nova versão da API.

4. Implemente a alteração e atualize a especificação OpenAPI. Mantenha o contrato documentado consistente com a validação, a persistência e o JSON efetivamente retornado. Registre a comparação entre os contratos anterior e proposto.

5. Adicione testes de API para a observação informada, sua recuperação e manutenção nas operações de pedidos, a ausência do campo, os tratamentos definidos e os limites de 500 e 501 caracteres. Preserve os testes existentes quando a mudança for compatível; qualquer alteração decorrente de uma incompatibilidade deverá ser justificada, sem eliminar a cobertura do contrato anterior mantido em uso.

## Restrições

- Limite a implementação ao requisito proposto e aos ajustes necessários para atendê-lo.
- Preserve as regras atuais de itens, valores, pagamento e entrega.
- Não crie outra aplicação, migre para microsserviços ou realize uma reestruturação geral. Nenhum padrão arquitetural é obrigatório.
- Não crie endpoints adicionais sem necessidade demonstrada. Uma rota `/v2` não é requisito da atividade.
- Mantenha as integrações simuladas, o pipeline e a cobertura mínima de 80% de linhas, sem novas exclusões no JaCoCo.

## Entrega

Atividade individual ou em dupla, com entrega por **Pull Request**, contendo:

- identificação dos integrantes;
- análise do contrato, consumidores, compatibilidade e decisão sobre versionamento;
- implementação, testes e especificação OpenAPI, com a comparação antes/depois registrada no PR ou no histórico de commits;
- resultado de `mvn clean verify`, cobertura obtida e evidência de validação da especificação OpenAPI.

**Em caso de trabalho em dupla, cada integrante deverá enviar individualmente o link do Pull Request no Studeo.**
