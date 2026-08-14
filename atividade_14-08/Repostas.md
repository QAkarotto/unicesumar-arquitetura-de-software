# Atividade dia 14/08

## Porque a organização é problemática

- conectar o frontend diretamente com o banco de dados pode ser problemático pois pode expor credenciais de acesso e informações sensíveis dos usuários que podem ser vazadas 

- os usuários podem conectar diretamente ao banco o que pode fazer com que usuários mal-itencionados podem manipular dados,alterar dados e deletar 

- Sem uma camada intermediária toda validação de pagamento pode ser facilmente adulterada pelo usuário antes de enviar ao PayPal ou salvar no banco

## Qual é a principal diferença entre as responsabilidades do frontend e do backend?

-  O principal papel do frontend é coletar os dados do usuários, todos inputs que são escritos, dados como localização, devem ser validados e tratados pelo front para envia-los para o backend onde terá mais uma camada de validações para mandar para o banco de dados

## Por que o frontend não deve acessar diretamente o banco de dados?

- O frontend não pode acessar diretamente o banco de dados pois não existem validações, autenticações  então qualquer um pode alterar dados e acessar dados sensíveis, os usuários precisam ser validados antes de fazer requisições importantes

## Qual é o papel da API na comunicação entre frontend e backend?

- O papel da API é ser o comunicador entre o frontend e o backend ele traz as informações do backend para o o frontend e trata os inputs do frontend para mandar o backend, é a camada intermediaria que é necessária para segurança e comunicação entre as partes.