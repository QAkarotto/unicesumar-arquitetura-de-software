# Guia rápido para entender o que foi alterado

Este guia é para estudo, não precisa ser enviado no Pull Request.

## 1. Leia estes três arquivos nesta ordem

1. `src/main/java/br/edu/foodnow/entregadores/ProvedorEntregadores.java` — a **interface**, que diz o que pode ser solicitado e o que volta como resultado.
2. `src/main/java/br/edu/foodnow/integration/FakeCourierAdapter.java` — a classe que **implementa a interface** e chama o simulador antigo.
3. `src/main/java/br/edu/foodnow/service/EntregaService.java` — a classe que usa a interface para pedir um entregador.

A ideia: o `EntregaService` não precisa saber exatamente como o simulador funciona. Ele só chama `solicitarEntregador`.

## 2. Um exemplo de como explicar ao professor

> Escolhi mexer na comunicação com o serviço de entregadores porque o `EntregaService` dependia diretamente do cliente simulado. Separei essa comunicação usando uma interface e um adaptador. Assim, se o simulador for trocado, a classe que cria a entrega não precisa conhecer os detalhes do novo fornecedor. Mantive o comportamento antigo e conferi com testes. Usei IA como apoio para entender e implementar essa mudança.

**Use esse exemplo somente depois de confirmar que você o compreende.** Não precisa decorar.

## 3. Exercício curto para fazer de verdade

- Abra `FakeCourierAdapterTest.java`.
- Escolha o teste da região central.
- Identifique quais valores entram na solicitação e quais resultados o teste espera.
- Mude temporariamente um valor esperado (por exemplo, `4` para `5`) e rode o teste específico: `mvn -Dtest=FakeCourierAdapterTest test`. Ele deverá falhar.
- Volte o valor original e rode `mvn clean verify`. A execução deve passar novamente.
- Registre com suas palavras o que aconteceu; isso demonstra que você sabe para que o teste serve.

## 4. Cuidado antes de enviar

A atividade é entregue por Pull Request, não apenas enviando o ZIP. Preencha os campos de integrante e link, verifique o relatório e não envie a pasta `target` (arquivos gerados). A base já veio com parte da refatoração pronta: mantenha isso transparente.
