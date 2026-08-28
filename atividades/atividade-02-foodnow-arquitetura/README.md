# Atividade 02 - Diagramas de Arquitetura (FoodNow)

Esta pasta contem os tres diagramas editaveis em draw.io para a atividade pratica:

- `diagrama-nivel-sistema.drawio`
- `diagrama-nivel-subsistema.drawio`
- `diagrama-nivel-componente-pedidos.drawio`

## Coerencia entre niveis

- O diagrama de sistema mostra contexto e fronteiras da FoodNow.
- O diagrama de subsistema detalha a estrutura interna da FoodNow.
- O diagrama de componente detalha internamente o subsistema Pedidos.

## Ajustes visuais recomendados no draw.io

1. Abrir cada arquivo em diagrams.net.
2. Inserir os icones/logos de tecnologia em cada elemento, mantendo tambem o nome da tecnologia:
   - Flutter / Dart
   - React / TypeScript
   - Java / Spring Boot
   - PostgreSQL
   - Redis
   - JWT
   - PayPal API
   - Google Maps Platform API
3. Revisar alinhamento, paleta e legibilidade dos conectores.
4. Exportar cada diagrama em PNG ou PDF.

## Sugestao de exportacao

- `foodnow-nivel-sistema.png` (ou `.pdf`)
- `foodnow-nivel-subsistema.png` (ou `.pdf`)
- `foodnow-nivel-componente-pedidos.png` (ou `.pdf`)

## Texto-base para o Pull Request

Integrantes:
- Nome 1
- Nome 2 (se aplicavel)

Decisao arquitetural relevante:
- O backend centraliza o acesso a dados e integracoes externas, garantindo que os aplicativos clientes nao acessem diretamente PostgreSQL/Redis e reduzindo acoplamento com servicos externos.

Reflexao sobre niveis de abstracao:
- No nivel de sistema, o foco foi contexto e fronteiras.
- No nivel de subsistema, o foco foi organizacao interna e responsabilidades principais.
- No nivel de componente, o foco foi o fluxo interno do subsistema Pedidos, evidenciando responsabilidades tecnicas e pontos de integracao.
