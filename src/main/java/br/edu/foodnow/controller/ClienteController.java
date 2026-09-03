package br.edu.foodnow.controller;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.ClienteResponse cadastrar(@Valid @RequestBody ApiDtos.ClienteRequest request) {
        return ApiDtos.ClienteResponse.from(clienteService.cadastrar(request.nome(), request.email()));
    }

    @GetMapping("/{id}")
    public ApiDtos.ClienteResponse consultar(@PathVariable Long id) {
        return ApiDtos.ClienteResponse.from(clienteService.consultar(id));
    }

    @PostMapping("/{id}/enderecos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.ClienteResponse adicionarEndereco(@PathVariable Long id,
                                                      @Valid @RequestBody ApiDtos.EnderecoRequest request) {
        Cliente cliente = clienteService.adicionarEndereco(id, request.toModel());
        return ApiDtos.ClienteResponse.from(cliente);
    }

    @PutMapping("/{clienteId}/enderecos/{enderecoId}/principal")
    public ApiDtos.ClienteResponse definirPrincipal(@PathVariable Long clienteId, @PathVariable Long enderecoId) {
        return ApiDtos.ClienteResponse.from(clienteService.definirEnderecoPrincipal(clienteId, enderecoId));
    }
}
