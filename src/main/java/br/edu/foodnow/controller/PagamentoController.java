package br.edu.foodnow.controller;

import br.edu.foodnow.service.PagamentoService;
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
@RequestMapping("/pagamentos")
public class PagamentoController {
    private final PedidoService pedidoService;
    private final PagamentoService pagamentoService;

    public PagamentoController(PedidoService pedidoService, PagamentoService pagamentoService) {
        this.pedidoService = pedidoService;
        this.pagamentoService = pagamentoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.PagamentoResponse processar(@Valid @RequestBody ApiDtos.PagamentoRequest request) {
        return ApiDtos.PagamentoResponse.from(
                pedidoService.pagar(request.pedidoId(), request.forma(), request.token()));
    }

    @GetMapping("/{id}")
    public ApiDtos.PagamentoResponse consultar(@PathVariable Long id) {
        return ApiDtos.PagamentoResponse.from(pagamentoService.consultar(id));
    }
}
