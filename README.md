# Integrantes
- Otavio Alves Brisky
- Felipe de Oliveira Guimarães

## Atividade 2: 
## Decisão Arquitetural

<p align="justify">
Foi adotado um Backend centralizado em Java/Spring Boot, responsável pelas regras de negócio, autenticação e integração com os serviços externos. Os aplicativos do cliente, restaurante e entregador comunicam-se com o Backend por REST/HTTP(S)/JSON, sem acesso direto ao banco de dados. O PostgreSQL é utilizado para persistência, o Redis para cache, o PayPal para pagamentos e o Google Maps Platform para localização e entregas. Essa abordagem reduz o acoplamento entre as aplicações, centraliza as regras de negócio e facilita a manutenção e evolução do sistema.
</p>

## Nível de Abstração

<p align="justify">
Os três diagramas foram desenvolvidos de forma progressiva, representando a mesma arquitetura da FoodNow em diferentes níveis de abstração. O Nível de Sistema apresenta a plataforma em seu contexto geral, destacando os principais atores e as interações com os serviços externos, como PayPal e Google Maps Platform.
No Nível de Subsistema, a estrutura interna da FoodNow é detalhada, apresentando as aplicações do cliente, restaurante e entregador, o Backend/API, os subsistemas de Usuários e Autenticação, Pedidos e Entregas, além das tecnologias e serviços utilizados.
Por fim, o Nível de Componente aprofunda o subsistema de Pedidos, demonstrando a comunicação entre o Controller, Serviço de Pedidos, Validador, Repositório e componente de integração com o PayPal, além das conexões com PostgreSQL.
A utilização dos três níveis permite compreender a arquitetura desde uma visão mais ampla até sua organização interna. Dessa forma, os diagramas mantêm coerência entre si e demonstram como os elementos apresentados no nível de sistema são detalhados progressivamente nos níveis de subsistema e componente.
</p>

## Atividade 3:
## Qual é a principal diferença entre as responsabilidades do frontend e do backend?

O frontend é responsável pela apresentação dos dados e pela interação com o usuário, enquanto o backend é responsável pelo processamento das informações, aplicação das regras de negócio, segurança, consultas e persistência dos dados no banco de dados.

## Por que o frontend não deve acessar diretamente o banco de dados?

O frontend não deve acessar diretamente o banco de dados por questões de segurança e controle. Essa prática faria com que as regras de negócio e as validações do backend fossem ignoradas, tornando a aplicação mais vulnerável e dificultando o controle sobre o acesso e a manipulação dos dados.

## Qual é o papel da API na comunicação entre frontend e backend?

A API atua como intermediária na comunicação entre o frontend e o backend. Ela recebe as solicitações realizadas pelo frontend, encaminha essas requisições para o backend, que aplica as regras de negócio e realiza os processamentos necessários, e posteriormente retorna a resposta para o frontend, normalmente utilizando dados no formato JSON.

