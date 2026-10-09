Atividade 04: Decisão Arquitetural e Trade-offs

Trabalho em Dupla
Felipe Augusto Graniska
Victor Gabriel Alves Carneiro

Sistema analisado: Nubank

1. Contexto

Escolhemos o Nubank porque é um banco digital que praticamente todo mundo usa ou conhece hoje em dia. Ele atende principalmente pessoas físicas que querem gerenciar conta, cartão de crédito, investimentos e outros serviços financeiros direto pelo app, sem precisar ir numa agência física. O grande problema que ele resolve é a burocracia e a lentidão dos bancos tradicionais — o Nubank oferece abertura de conta rápida, atendimento pelo app e uma experiência bem mais simples do que a de um banco convencional.

2. Requisitos Arquiteturalmente Significativos
O sistema precisa proteger dados financeiros e pessoais dos clientes, já que estamos falando de informações extremamente sensíveis (saldo, transações, dados de cartão).
Precisa ficar disponível o tempo todo, porque as pessoas usam o app pra pagar contas, fazer Pix e movimentar dinheiro a qualquer hora, inclusive fora do horário comercial.
Precisa processar transações (como Pix) rapidamente, já que o usuário espera ver o dinheiro sair e entrar quase na hora.
3. Atributos de Qualidade
Requisito	Atributo de Qualidade
Proteção de dados financeiros	Segurança
Disponibilidade contínua do app	Disponibilidade
Processamento rápido de transações (Pix, pagamentos)	Desempenho
4. Priorização

Entre os três, escolhemos Segurança como o atributo mais importante pra esse tipo de sistema.

O motivo é simples: um banco digital lida direto com dinheiro e dados pessoais das pessoas. Se o app ficar fora do ar por alguns minutos é ruim, mas dá pra contornar. Agora, se acontecer um vazamento de dados ou alguém conseguir acessar a conta de outra pessoa, o problema não tem volta — a confiança do cliente é destruída e pode virar até um caso jurídico envolvendo o banco. Por isso achamos que segurança tem que vir na frente dos outros atributos, mesmo que isso signifique abrir mão de um pouco de velocidade em algumas operações.

5. Decisão Arquitetural

Decisão: usar autenticação multifator (MFA) combinada com criptografia de ponta a ponta nas transações, além de validações extras (como biometria) para operações mais sensíveis, tipo aumentar limite do cartão ou fazer um Pix de valor alto.

Problema que resolve: evitar que alguém consiga acessar a conta ou autorizar uma transação só descobrindo a senha do usuário, o que é bem mais fácil de acontecer do que muita gente imagina (vazamento de senha, phishing, etc).
Benefício: mesmo que a senha vaze, o invasor ainda precisaria de outro fator (celular, biometria) pra completar a ação, o que reduz muito a chance de fraude.
Trade-off: a experiência do usuário fica um pouco mais lenta e trabalhosa, porque toda operação sensível exige uma etapa a mais de verificação. Isso pode até incomodar alguns clientes no dia a dia, mas é o preço que se paga pra manter o sistema seguro — e no caso de um banco, esse trade-off vale muito a pena.