# FoodNow

FoodNow é uma aplicação monolítica de delivery usada em atividades práticas da disciplina de Arquitetura de Software. A API permite cadastrar clientes, endereços, restaurantes e produtos, criar e confirmar pedidos, processar pagamentos e acompanhar entregas.

O projeto funciona inteiramente offline: mapas, pagamentos e e-mails são simulados localmente e o banco H2 é criado em memória.

## Requisitos

- JDK 26;
- Maven 3.9+.

## Executar

```bash
mvn spring-boot:run
```

A API fica disponível em `http://localhost:8080`. O console H2 pode ser acessado em `http://localhost:8080/h2-console`, usando a URL JDBC `jdbc:h2:mem:foodnow`, usuário `sa` e senha vazia.

## Testes e verificação

Executar os testes unitários:

```bash
mvn test
```

Executar compilação, testes unitários, testes de integração, relatório e validação de cobertura:

```bash
mvn clean verify
```

O relatório HTML do JaCoCo é gerado em `target/site/jacoco/index.html`. O build exige cobertura mínima de 80% das linhas.

## Principais endpoints

| Método | Endpoint | Operação |
|---|---|---|
| `POST` | `/clientes` | Cadastrar cliente |
| `GET` | `/clientes/{id}` | Consultar cliente |
| `POST` | `/clientes/{id}/enderecos` | Cadastrar endereço |
| `PUT` | `/clientes/{clienteId}/enderecos/{enderecoId}/principal` | Definir endereço principal |
| `POST` | `/restaurantes` | Cadastrar restaurante |
| `GET` | `/restaurantes/{id}` | Consultar restaurante |
| `POST` | `/restaurantes/{id}/produtos` | Cadastrar produto |
| `GET` | `/produtos/{id}` | Consultar produto |
| `PATCH` | `/produtos/{id}/disponibilidade` | Alterar disponibilidade |
| `POST` | `/pedidos` | Criar pedido com itens |
| `POST` | `/pedidos/{id}/itens` | Adicionar item |
| `POST` | `/pedidos/{id}/confirmar` | Confirmar pedido |
| `GET` | `/pedidos/{id}` | Consultar pedido |
| `POST` | `/pagamentos` | Processar pagamento |
| `GET` | `/pagamentos/{id}` | Consultar pagamento |
| `GET` | `/entregas/{id}` | Consultar entrega |
| `GET` | `/entregas/pedido/{pedidoId}` | Consultar entrega por pedido |
| `PATCH` | `/entregas/{id}/status` | Atualizar status da entrega |

Para simular uma rejeição no pagamento, envie o token `REJEITADO`. Qualquer outro token não vazio produz aprovação determinística.
