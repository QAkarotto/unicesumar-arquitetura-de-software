# Atividade 1 - 2° Bimestre
APIs como Fronteiras Arquiteturais

## Análise dos métodos atuais 

- **POST /pedidos**: Criar um pedido informando cliente, restarante, endereço e itens.  - **DTO de entrada** 
- **GET /pedidos/{id}**: Consultar os dados de um pedido pelo id. - **DTO de resposta** 
- **POST /pedidos/{id}/itens**: Adicionar itens no pedido que já existe pelo id. - **DTO de entrada** 
- **POST /pedidos/{id}/confirmar**: Confirmar pedido - **Teste para verificar se as informações estão corretas** 

Os parâmetros são as informações passadas na URL nesses métodos é o {id}, essas informações são enviadas para API por um JSON e a API devolve algumas informações para o cliente. As validações são as regras que os dados precisam cumprir no nosso caso a observação é opcional mas precisa ter no máximo 500 caracteres. Os códigos HTTP identificam o resultado dessa chamada. 

## Especificação OpenAPI
Foi criada uma especificação em YAML para documentar as operações (métodos HTTP, caminhos, parametros,etc), a especificação original foi preservada para comparação e para o histórico. 

## Proposta de evolução do contrato
Como proposta para adicionar o campo observação, é necessário modificar alguns compontentes: PedidoRequest, PedidoResponse e Pedido. Nesse campo ele poderá ir como vazio (null) ou com caracteres sendo até 500 caracteres, caso ultrapasse desse valor o pedido será rejeitado

Anteriormente esse era o layout: 

``` {
  "clienteId": 1,
  "restauranteId": 2,
  "enderecoEntregaId": 3,
  "itens": [
    {
      "produtoId": 4,
      "quantidade": 1
    }
  ]
} 
``` 

Com o campo observaçao: 


```{
  "clienteId": 1,
  "restauranteId": 2,
  "enderecoEntregaId": 3,
  "itens": [
    {
      "produtoId": 4,
      "quantidade": 1
    }
  ],
  "observacao": "Não incluir talheres"
}
```
## Implementação e testes
Na implementação foi adicionada no arquivo ApiDtos.java foi adicionado o campo de observações no PedidoRequest: 
```
@Size(max = 500)
String observacao
``` 

No PedidoResponse foi adicionada uma instrução para retornar a observação do pedido: 

```
pedido.getObservacao()
```

Após as implentações realizamos alguns testes para validar os campos e esse foi o retorno: 
``` 
[INFO] Running br.edu.foodnow.integration.IntegracoesSimuladasTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.385 s -- in br.edu.foodnow.integration.IntegracoesSimuladasTest
[INFO] Running br.edu.foodnow.integration.MapsLocalizacaoAdapterTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.014 s -- in br.edu.foodnow.integration.MapsLocalizacaoAdapterTest
[INFO] Running br.edu.foodnow.logistica.PoliticaEntregaAtualTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.061 s -- in br.edu.foodnow.logistica.PoliticaEntregaAtualTest
[INFO] Running br.edu.foodnow.model.LocalizacaoEntregaTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.055 s -- in br.edu.foodnow.model.LocalizacaoEntregaTest
[INFO] Running br.edu.foodnow.model.PedidoTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.040 s -- in br.edu.foodnow.model.PedidoTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
```



