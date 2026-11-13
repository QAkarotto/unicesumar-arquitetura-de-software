# Atividade 04 - 2º Bim
Resiliência e Observabilidade no FoodNow

## Fluxo existente escolhido 
Escolhi o EntregaService o código atual recebe NO_COURIER_AVAIABLE mas não verifica o retorno e salva a entrega com o status de "Aguardando Entregador"

## Simulação da falha
Foram realisadas chamadas nas requisições para verificar a falha: 
- Chamada da API após a criação do pedido: 

```
http://localhost:8080/entregas/pedido/2
```
- Retorno da API

```
{
  "id": 2,
  "pedidoId": 2,
  "enderecoId": 1,
  "distanciaKm": 63.94,
  "tempoEstimadoMinutos": 236,
  "status": "AGUARDANDO_ENTREGADOR",
  "zonaEntrega": "EXPANDIDA",
  "statusDespachoExterno": "NO_COURIER_AVAILABLE",
  "custoOperacional": 57.77
}
```


## Melhoria 

Para correção foi criado um status de entrega e a verificação do status para retornar a situação real da entrega do cliente. 

- Status
```
public enum StatusEntrega {
    AGUARDANDO_ENTREGADOR,
    EM_ROTA,
    ENTREGUE,
    FALHA_NO_DESPACHO
}
```

- Criar planejamento de entrega e retornar status 
```
 public Entrega criarPara(Pedido pedido) {
        var plano = logisticaService.planejarEntrega(pedido);

        FakeCourierClient.CourierRequest requisicao =
                new FakeCourierClient.CourierRequest(
                        pedido.getId(),
                        plano.distanciaKm(),
                        plano.zona(),
                        pedido.getEnderecoEntrega()
                                .getLocalizacao()
                                .formatarParaProvedor()
                );

        log.info(
                "Iniciando despacho de entrega. pedidoId={}, distanciaKm={}, zona={}",
                pedido.getId(),
                plano.distanciaKm(),
                plano.zona()
        );

        FakeCourierClient.CourierDispatch despacho =
                courierClient.solicitarEntregador(requisicao);

        int tempo = plano.tempoBaseMinutos()
                + despacho.pickupEtaMinutes();

        Entrega entrega = new Entrega(
                pedido,
                pedido.getEnderecoEntrega(),
                plano.distanciaKm(),
                tempo,
                plano.zona(),
                despacho.courierCode(),
                despacho.providerStatus()
        );

        if ("NO_COURIER_AVAILABLE".equals(despacho.providerStatus())) {
            entrega.marcarFalhaNoDespacho();

            log.warn(
                    "Falha no despacho da entrega. pedidoId={}, tipoFalha={}, tratamento={}",
                    pedido.getId(),
                    despacho.providerStatus(),
                    "ENTREGA_REGISTRADA_COM_FALHA_DE_DESPACHO"
            );
        } else {
            log.info(
                    "Entregador atribuído com sucesso. pedidoId={}, statusDespacho={}",
                    pedido.getId(),
                    despacho.providerStatus()
            );
        }

        return entregaRepository.save(entrega);
    }
```

## Testes após a implementação 

- Chamada da API
```
http://localhost:8080/entregas/pedido/1
```

- Retorno do erro com o status e a explicação
```
{
  "id": 1,
  "pedidoId": 1,
  "enderecoId": 1,
  "distanciaKm": 63.94,
  "tempoEstimadoMinutos": 236,
  "status": "FALHA_NO_DESPACHO",
  "zonaEntrega": "EXPANDIDA",
  "statusDespachoExterno": "NO_COURIER_AVAILABLE",
  "custoOperacional": 57.77
}
```

## Resultado 

```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  04:53 min
[INFO] Finished at: 2026-11-13T20:18:39-03:00

```
