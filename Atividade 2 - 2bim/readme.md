# Atividade 02 - 2º Bim
Dependency Rule e Limites Arquiteturais no FoodNow

## Analise da dependência

### Dependência escolhida 
- A dependência escolhida foi a **EntregaService** que é a integração direta com os entregadores. 

### Classes envolvidas 
- **EntregaService**: Onde estão as regras relacionadas as entregas (Criação, Consulta e Atualização)
- **FakeCourierClient**: Onde é realizada a comunicação dos entregadores com o pedido

### Onde ela aparece? 
- A classe EntregaService depende da implementação do FakeCoutierClient, ela aparece no atributo courierClient, no método criarPara(). 

### Problema 
- A classe EntregaService usa diretamente a classe FakeCoutierClient, ou seja, ela acessa todos os detalhes da classe, e isso é um problema pois se tiver alguma mudança na FakeCoutierClient a EntregaService tambem vai precisar fazer alterações, isso acaba causando retrabalho e alteraçoes desnecessarias. 

## Proposta 
- Criar uma interface para mostrar somente os dados que interessam a clase, a classe ainda vai depender da outra mas somente para os serviços certos. 

## Implementação 

- Criação de uma interface: 
```
public interface CourierClient {}
```
- Chamada da classe: 
```
public class FakeCourierClient implements CourierClient {} 
```
- Serviço dependendo da classe
```
public class EntregaService {

    private final CourierClient courierClient;

    public EntregaService(CourierClient courierClient) {
        this.courierClient = courierClient;
    }
}
```

## Resultado dos testes

```
BUILD SUCCESS
```

