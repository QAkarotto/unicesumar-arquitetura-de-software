package br.edu.foodnow.controller;

import br.edu.foodnow.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.PedidoResponse criar(@Valid @RequestBody ApiDtos.PedidoRequest request) {
        var itens = request.itens().stream()
                .map(item -> new PedidoService.ItemSolicitado(item.produtoId(), item.quantidade())).toList();
        return ApiDtos.PedidoResponse.from(pedidoService.criar(request.clienteId(), request.restauranteId(),
                request.enderecoEntregaId(), itens));
    }

    @GetMapping("/{id}")
    public ApiDtos.PedidoResponse consultar(@PathVariable Long id) {
        return ApiDtos.PedidoResponse.from(pedidoService.consultar(id));
    }

    @PostMapping("/{id}/itens")
    public ApiDtos.PedidoResponse adicionarItem(@PathVariable Long id,
            @Valid @RequestBody ApiDtos.ItemPedidoRequest request) {
        return ApiDtos.PedidoResponse.from(pedidoService.adicionarItem(id, request.produtoId(),
                request.quantidade()));
    }

    @PostMapping("/{id}/confirmar")
    public ApiDtos.PedidoResponse confirmar(@PathVariable Long id) {
        return ApiDtos.PedidoResponse.from(pedidoService.confirmar(id));
    }
}
