# Arquitetura de Software: gabarito das 30 questões de estudo

Respostas esperadas. Nas questões de análise, outras soluções são válidas quando coerentes com o contexto e acompanhadas de justificativa.

## Decisões arquiteturais e requisitos

### Questão 1
Uma decisão arquitetural influencia a estrutura, os limites, as dependências ou propriedades relevantes do sistema. Exemplo: processar notificações de forma assíncrona para que a confirmação de pedidos não dependa do envio imediato de e-mails.

### Questão 2
A adequação depende do contexto. Entre os fatores estão demanda, orçamento, prazo, tamanho e experiência da equipe, riscos e integrações existentes. Uma solução que oferece autonomia para várias equipes pode impor custos desnecessários a uma equipe pequena.

### Questão 3
Requisito funcional descreve um comportamento, como registrar um pedido. Atributo de qualidade descreve uma propriedade desse comportamento ou do sistema, como desempenho. Exemplo de requisito de qualidade: registrar pedidos em até dois segundos sob uma carga definida.

### Questão 4
É um requisito com impacto importante sobre a estrutura ou as decisões arquiteturais. Pode ser funcional, de qualidade ou envolver uma restrição. Por exemplo, dividir um pagamento entre vários recebedores é uma funcionalidade que pode afetar integrações, dados e transações.

### Questão 5
A solução simples tende a ser mais adequada, pois reduz esforço inicial e custo operacional, favorecendo a entrega no prazo. A solução antecipadamente complexa pode oferecer capacidade futura, mas exige investimento antes de haver evidências de necessidade. O trade-off da simplicidade é aceitar possíveis adaptações futuras, mantendo limites que facilitem a evolução.

### Questão 6
Trade-off é a relação entre benefícios e custos de uma escolha. Instâncias redundantes podem favorecer disponibilidade, permitindo que uma instância assuma o atendimento quando outra falha. Em contrapartida, aumentam custo e exigem mecanismos de distribuição de tráfego, detecção de falhas e tratamento do estado compartilhado.

## Atributos de qualidade

### Questão 7
- **Desempenho:** tempo de resposta, volume de processamento e uso de recursos sob determinadas condições.
- **Escalabilidade:** capacidade de atender ao crescimento da carga com ampliação de recursos.
- **Elasticidade:** capacidade de ajustar recursos à variação da demanda, ampliando-os ou reduzindo-os.

### Questão 8
- Responder rapidamente: desempenho.
- Restringir acessos indevidos: segurança.
- Continuar operando durante falhas: tolerância a falhas, favorecendo confiabilidade e disponibilidade.
- Facilitar novas integrações: manutenibilidade, especialmente modificabilidade; interoperabilidade também é relevante para a troca de informações.

### Questão 9
Disponibilidade indica que o sistema está acessível para uso. Isso não garante preservação dos registros. O cenário também exige durabilidade dos dados confirmados e mecanismos de recuperação, como persistência adequada, cópias de segurança e procedimentos de restauração conforme os requisitos de perda e recuperação toleráveis.

### Questão 10
- Atender acessos simultâneos mantendo resposta aceitável: desempenho e capacidade; escalabilidade quando for necessário ampliar recursos para suportar o crescimento.
- Preservar respostas confirmadas: durabilidade dos dados e confiabilidade.
- Permitir acesso somente a estudantes autorizados: segurança, com autenticação e autorização.

### Questão 11
Uma priorização defensável é **segurança e confiabilidade**, devido aos riscos de acesso indevido, perda de registros e processamento incorreto ou duplicado. Disponibilidade e desempenho também são importantes. Outras escolhas são aceitáveis se explicarem os impactos para o negócio e não tratarem requisitos mínimos dos demais atributos como dispensáveis.

### Questão 12
Considerando segurança e confiabilidade, uma possibilidade é concentrar a confirmação de pagamentos em um serviço de aplicação com autorização, transações e controle de idempotência. Isso ajuda a impedir operações não permitidas, gravações incompletas e processamento duplicado de uma mesma solicitação. O trade-off envolve maior complexidade de implementação, armazenamento de informações de controle e possível custo de processamento. Para integrações externas, são necessários mecanismos adicionais de coordenação e reconciliação.

## Coesão, acoplamento e modularidade

### Questão 13
A classe reúne responsabilidades com diferentes motivos para mudar, indicando baixa coesão. Uma melhoria é separar cálculo de preços, notificações, relatórios e persistência, mantendo a coordenação do caso de uso em um componente apropriado. A separação deve seguir responsabilidades, sem exigir uma classe ou um serviço independente para cada método.

### Questão 14
Coesão é a relação entre as responsabilidades dentro de um componente. Acoplamento é o grau e a natureza da dependência entre componentes. Responsabilidades relacionadas e dependências controladas favorecem compreensão, testes e mudanças localizadas. Um componente pode ser coeso e ainda depender excessivamente de detalhes de outros.

### Questão 15
Pode indicar alto acoplamento a detalhes internos de Clientes. Uma melhoria é encapsular esses detalhes e oferecer contratos explícitos para as operações necessárias. Assim, mudanças internas que preservem o contrato tendem a não exigir alterações nos consumidores. Mudanças no próprio contrato ainda podem afetá-los.

### Questão 16
Esses pacotes separam papéis técnicos, mas não necessariamente delimitam funcionalidades de negócio. Regras de pedidos podem continuar espalhadas ou misturadas a regras de pagamento e estoque. Boa modularidade também exige responsabilidades claras, encapsulamento e controle das dependências.

### Questão 17
Uma organização possível inclui **Pedidos**, responsável pelo ciclo de vida do pedido; **Pagamentos**, responsável pela cobrança e pelo acompanhamento de seu estado; e **Entregas**, responsável por regras de prazo e acompanhamento da entrega. Cada módulo concentra suas regras e expõe operações necessárias aos demais, podendo possuir camadas técnicas internas.

### Questão 18
A duplicação pode causar resultados divergentes, correções repetidas e propagação de mudanças. A regra deve ficar no módulo responsável por entregas. Os demais módulos devem solicitar ou receber o resultado por contratos explícitos, em vez de reproduzir o cálculo ou acessar sua implementação interna.

### Questão 19
Contratos definem operações disponíveis, dados de entrada, resultados e erros relevantes. O consumidor depende dessas definições, enquanto o módulo mantém sua implementação encapsulada. Por exemplo, Pedidos pode solicitar uma estimativa de entrega sem conhecer o algoritmo utilizado.

### Questão 20
Não. O consumidor continua dependendo do contrato, de seus dados e de seu comportamento. Uma interface pode reduzir a dependência de uma implementação concreta, mas contratos instáveis ou que exponham detalhes internos continuam gerando acoplamento elevado.

## Arquitetura web

### Questão 21
- **Frontend:** apresenta informações, recebe entradas e envia solicitações.
- **Backend:** executa casos de uso, aplica regras de negócio e controla permissões.
- **Persistência:** armazena e recupera dados.

Validações no frontend podem melhorar a experiência, mas não substituem validações e controles no backend.

### Questão 22
O frontend fica acoplado ao esquema do banco, de modo que alterações nas tabelas podem exigir mudanças na interface. Além disso, o acesso pode expor credenciais e permitir operações sem os controles adequados de negócio e segurança. Uma alternativa é uma API no backend, que controla o acesso e mantém os detalhes de persistência encapsulados.

## Monólito modular e microsserviços

### Questão 23
No monólito modular, os módulos possuem limites internos, mas normalmente fazem parte de uma mesma unidade de implantação e se comunicam dentro do processo. Em microsserviços, os serviços podem ser implantados separadamente e se comunicam por mecanismos remotos, como APIs ou mensagens. Essa separação introduz latência e situações de falha de comunicação.

### Questão 24
É necessário medir carga, tempo de resposta e consumo de recursos; localizar operações lentas; analisar consultas ao banco, integrações externas, bloqueios e configurações. Também devem ser consideradas necessidades de escala independente, capacidade operacional, equipe e custo. A decisão deve partir do gargalo e dos requisitos identificados.

### Questão 25
Analisar o plano de execução da consulta, avaliar um índice adequado e medir o resultado sob carga representativa. As evidências justificam tratar primeiro o gargalo identificado, mantendo inicialmente a arquitetura. Um índice também tem custos de armazenamento e escrita. Migrar para microsserviços não resolveria automaticamente a consulta lenta.

### Questão 26
A adoção pode ser justificada por limites de negócio claros, necessidade real de implantação ou escala independente e capacidade da equipe para operar serviços distribuídos. Dois benefícios potenciais são autonomia de implantação e escala por serviço. Dois custos são tratamento de falhas de rede e maior complexidade de monitoramento e operação. Consistência entre dados distribuídos é outro desafio possível.

## Representações arquiteturais

### Questão 27
Os públicos têm objetivos diferentes. Gestores tendem a precisar de limites do sistema, integrações, custos e riscos. Desenvolvedores podem precisar de aplicações, módulos e contratos. Quem implementa uma funcionalidade pode precisar de classes, interfaces e interações detalhadas. O nível deve ser escolhido pela finalidade da comunicação; esses públicos e suas necessidades podem se sobrepor.

### Questão 28
- Usuários e sistemas externos: **Contexto**.
- Aplicação web, API e banco de dados: **Containers**.
- Componentes internos da API: **Componentes**.
- Classes e interfaces: **Código**.

No C4, container representa uma aplicação ou armazenamento de dados, não necessariamente um container Docker.

### Questão 29
Um diagrama de classes prioriza detalhes de implementação e pode ocultar as relações relevantes para essa discussão. Um diagrama de contexto é um ponto de partida mais adequado para mostrar usuários, sistema e parceiros externos. Para orçamento, podem ser acrescentadas informações de containers, implantação, capacidade e custos conforme necessário.

### Questão 30
A visão estrutural mostra partes do sistema e suas relações, como os componentes de Pedidos e Pagamentos e o contrato entre eles. A visão de sequência mostra a ordem das interações em uma operação, como receber uma solicitação, validar o pedido, solicitar pagamento e devolver o resultado. As duas visões são complementares.
