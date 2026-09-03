package br.edu.foodnow.controller;

import br.edu.foodnow.model.Produto;
import br.edu.foodnow.service.RestauranteService;
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
@RequestMapping("/restaurantes")
public class RestauranteController {
    private final RestauranteService restauranteService;

    public RestauranteController(RestauranteService restauranteService) {
        this.restauranteService = restauranteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.RestauranteResponse cadastrar(@Valid @RequestBody ApiDtos.RestauranteRequest request) {
        return ApiDtos.RestauranteResponse.from(restauranteService.cadastrar(request.nome(),
                request.raioEntregaKm(), request.endereco().toModel()));
    }

    @GetMapping("/{id}")
    public ApiDtos.RestauranteResponse consultar(@PathVariable Long id) {
        return ApiDtos.RestauranteResponse.from(restauranteService.consultar(id));
    }

    @PostMapping("/{id}/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.ProdutoResponse cadastrarProduto(@PathVariable Long id,
                                                     @Valid @RequestBody ApiDtos.ProdutoRequest request) {
        Produto produto = restauranteService.cadastrarProduto(id, request.nome(), request.preco(),
                request.disponivel());
        return ApiDtos.ProdutoResponse.from(produto);
    }
}
