package br.edu.foodnow.controller;

import br.edu.foodnow.service.RestauranteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {
    private final RestauranteService restauranteService;

    public ProdutoController(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @GetMapping("/{id}")
    public ApiDtos.ProdutoResponse consultar(@PathVariable Long id) {
        return ApiDtos.ProdutoResponse.from(restauranteService.consultarProduto(id));
    }

    @PatchMapping("/{id}/disponibilidade")
    public ApiDtos.ProdutoResponse alterarDisponibilidade(@PathVariable Long id,
            @Valid @RequestBody ApiDtos.DisponibilidadeRequest request) {
        return ApiDtos.ProdutoResponse.from(restauranteService.alterarDisponibilidade(id, request.disponivel()));
    }
}
