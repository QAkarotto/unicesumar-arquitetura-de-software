# Atividade 03 - 2º Bim
Integrações Externas e Tratamento de Falhas no FoodNow

### Identificação do serviço 
- A dependência escolhida foi a **EntregaService** que é a integração direta com os entregadores. 

### Cenários de Falha
- **Indisponibilidade de entregadores**: Quando não há entregador disponivel, no código atual ele retorna que não possuem entregadores mas mesmo assim continua no processamento do pedido. 
- **Serviço dos entregadores sem comunicação**: O sistema ainda não possui uma tratativa caso haja entregadores disponíveis, mas haja alguma falha durante a comunicação entre os sistemas. 

## Melhoria 
- Um novo arquivo para se houver uma falha de comunicação o sistema identificar o problema de integração
```
public class CourierIntegrationException extends RuntimeException {
    public CourierIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- Informar o erro para API quando não tiver entregadores
```
@ExceptionHandler(CourierIntegrationException.class)
public ResponseEntity<Map<String, String>>
        handleCourierIntegrationException(
                CourierIntegrationException ex) {
    Map<String, String> body = Map.of(
        "code", "COURIER_UNAVAILABLE",
        "message", "Não foi possível confirmar o entregador. "
                 + "O processamento do pedido foi interrompido."
    );
    return ResponseEntity
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(body);
}
```
- Consultar se possui entregadores 3 vezes, caso não tenha nenhum disponível o pedido é cancelado 

```
    for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
        despacho = courierClient.solicitarEntregador(requisicao);
        if ("NO_COURIER_AVAILABLE".equals(despacho.providerStatus())) {
            continue;
        }
        break;
    }

    if (despacho == null
            || "NO_COURIER_AVAILABLE".equals(despacho.providerStatus())) {
        cancelarPedido(pedido);
        throw new CourierIntegrationException(
            "Pedido cancelado após 3 tentativas sem entregador disponível.",
            null
        );
    }
```

## Benefício e o Trade-off
- **Benefício:** Fica mais coerente no sistema a alteração de que se não tiver entregadores o pedido é cancelado, pois como estava anteriormente o sistema estava calculando rota sem ter o entregador 
- **Trade-offs:** Isso pode aumentar o tempo de processamento e o número de chamadas ao sistema dos entregadores. 



## Resultado dos testes

- Teste para verificar a exceção 
```
    @Test
    void deveIdentificarFalhaNaIntegracaoComEntregadores() {
        RuntimeException causa = new RuntimeException("Timeout");

        CourierIntegrationException exception =
                new CourierIntegrationException(
                        "Falha na comunicação com entregadores",
                        causa
                );

        assertEquals(
                "Falha na comunicação com entregadores",
                exception.getMessage()
        );

        assertSame(causa, exception.getCause());
    }
```

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  33.244 s
[INFO] Finished at: 2026-10-27T19:47:41-03:00
```

