package br.edu.foodnow.controller;

import br.edu.foodnow.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/entregas")
public class EntregaController {
    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @GetMapping("/{id}")
    public ApiDtos.EntregaResponse consultar(@PathVariable Long id) {
        return ApiDtos.EntregaResponse.from(entregaService.consultar(id));
    }

    @GetMapping("/pedido/{pedidoId}")
    public ApiDtos.EntregaResponse consultarPorPedido(@PathVariable Long pedidoId) {
        return ApiDtos.EntregaResponse.from(entregaService.consultarPorPedido(pedidoId));
    }

    @PatchMapping("/{id}/status")
    public ApiDtos.EntregaResponse atualizarStatus(@PathVariable Long id,
            @Valid @RequestBody ApiDtos.AtualizarEntregaRequest request) {
        return ApiDtos.EntregaResponse.from(entregaService.atualizarStatus(id, request.status()));
    }
}
