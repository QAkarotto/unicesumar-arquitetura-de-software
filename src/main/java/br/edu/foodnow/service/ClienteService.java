package br.edu.foodnow.service;

import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final LocalizacaoService localizacaoService;

    public ClienteService(ClienteRepository clienteRepository, LocalizacaoService localizacaoService) {
        this.clienteRepository = clienteRepository;
        this.localizacaoService = localizacaoService;
    }

    public Cliente cadastrar(String nome, String email) {
        return clienteRepository.save(new Cliente(nome, email));
    }

    public Cliente consultar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
    }

    @Transactional
    public Cliente adicionarEndereco(Long clienteId, Endereco endereco) {
        Cliente cliente = consultar(clienteId);
        Localizacao coordenadas = localizacaoService.buscarCoordenadas(endereco);
        Endereco enderecoNormalizado = new Endereco(endereco.getLogradouro(), endereco.getNumero(),
                endereco.getBairro(), endereco.getCidade(), endereco.getCep(), coordenadas);
        cliente.adicionarEndereco(enderecoNormalizado);
        return clienteRepository.save(cliente);
    }

    @Transactional
    public Cliente definirEnderecoPrincipal(Long clienteId, Long enderecoId) {
        Cliente cliente = consultar(clienteId);
        try {
            cliente.definirEnderecoPrincipal(enderecoId);
        } catch (IllegalArgumentException excecao) {
            throw new RegraNegocioException(excecao.getMessage());
        }
        return clienteRepository.save(cliente);
    }
}
