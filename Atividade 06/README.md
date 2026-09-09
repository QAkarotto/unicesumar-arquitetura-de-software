# Atividade 06
Modularidade, Coesão e Acoplamento no FoodNow

## Localizar pontos

### Classe - Entrega
- **Método**: calcularDistancia()
- **Responsabilidade**: Calcular distância entre o restaurante e o endereço do cliente 
- **Dependencias**: Pedido, restaurante, cliente, localização
- **Problema**: Possui dois métodos para calcular distância, utilizando formas diferentes de cálculos
- **Mudança**: Atribuir o tipo de cálculo para o método calcularDistanciaPeloEndereco() 

### Classe - Endereço
- **Método**: classificarZonaDeEntrega()
- **Responsabilidade**: Classificar uma zona de entrega 
- **Dependencias**: bairro e cidade 
- **Problema**: Usa zonas pré definidas, se tiver alguma região nova ele pode calcular errado 
- **Mudança**: Modificar o método sempre que hover mudanças na região 

### Classe - Item Pedido
- **Método**: calcularParcelaGeografica()
- **Responsabilidade**: Calcula um valor adicional relacionado a região de destino e sobre a quantidade do produto
- **Dependencias**: Produto, endereço, região 
- **Problema**: O cálculo está relacionado a muitas classes
- **Mudança**: Alterar mais de uma classe para manter o cálculo 

### Classe - Notificação
- **Método**: adicionarReferenciaGeografica()
- **Responsabilidade**: Adiciona informações do endereõ na mensagem de notificação 
- **Dependencias**: Endereço, localização
- **Problema**: Possui depêndencia e regras relacionadas a localização 
- **Mudança**: Caso a forma de enviar informações de localização seja alterada, terá que ser alterada na notificação 

### Classe - Pagamento
- **Método**: possuiRiscoGeografico()
- **Responsabilidade**: Verifica se o pagamento possui risco com base na distância da entrega 
- **Dependencias**: Distancia, região 
- **Problema**: O pagamento precisa conhecer os conceitos geográficos e regiões não salvas
- **Mudança**: Caso a definição de risco seja alterada, será necessário modificar a classe de pagamento 

### Classe - Localização
- **Método**: calcularDistanciaManhattan()
- **Responsabilidade**: Calcular distância entre duas localizações utilizando latitude e longitude
- **Dependencias**: Localização
- **Problema**: Mais uma forma de calcular a distância 
- **Mudança**: Caso tenha alguma mudança precisa alterar em outros métodos tambem 

### Classe - Produto 
- **Método**: podeSerEntregueEm()
- **Responsabilidade**: Verificar se o produto está disponivel e se o restaurante pode chegar ao destino 
- **Dependencias**: Restaurante, endereço 
- **Problema**: Depende da classe resturante para verificar se a área de entrega pode ser entregue
- **Mudança**: Caso tenha alguma mudança precisa alterar em outros métodos tambem 

### Classe - Restaurante
- **Método**: calcularTaxaEntrega()
- **Responsabilidade**: Calcular taxa de entrega 
- **Dependencias**: Endereço
- **Problema**: Possui diferentes métodos para calcular a taxa de entrega
- **Mudança**: Caso a regra seja modificada, precisamos mudar os demais calculos 

## Localizar pontos

### Pedido 
Criar Pedido -> Confirmar -> Pagar -> Entrega 

### Pagamento  
Processar Pagamento -> Calcular Rota -> Registrar Pagamento -> Consultar Pagamento
- Eu deixaria o fluxo para calcular rota antes de processar o pagamento 

### Cliente 
Cadastrar -> Adicionar Endereço -> Atendimento

### Entrega 
Criar Entrega -> Consultar -> Consultar Por Pedido -> Atualizar Status 
- Eu deixaria consultar por pedido direto para economizar código 


## Refatoração 
- Organizei as responsabilidades para facilitar futuras alterações, nessa alteração o tempo de execução diminuiu

- **Primeiro teste** <br>
[INFO] Total time:  01:08 min <br>
[INFO] Finished at: 2026-09-08T20:43:01-03:00

- **Teste após alteração** <br>
[INFO] Total time:  39.987 s <br>
[INFO] Finished at: 2026-09-08T21:52:20-03:00 <br>


