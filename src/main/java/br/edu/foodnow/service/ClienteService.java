package br.edu.foodnow.service;

import br.edu.foodnow.localizacao.MapaGateway;
import br.edu.foodnow.localizacao.PoliticaAreaAtendimento;
import br.edu.foodnow.localizacao.Rota;
import br.edu.foodnow.model.Cliente;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.repository.ClienteRepository;
import br.edu.foodnow.repository.RestauranteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final RestauranteRepository restauranteRepository;
    private final MapaGateway mapaGateway;
    private final PoliticaAreaAtendimento politicaAreaAtendimento;

    public ClienteService(ClienteRepository clienteRepository, RestauranteRepository restauranteRepository,
                          MapaGateway mapaGateway, PoliticaAreaAtendimento politicaAreaAtendimento) {
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.mapaGateway = mapaGateway;
        this.politicaAreaAtendimento = politicaAreaAtendimento;
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
        Localizacao coordenadas = mapaGateway.geocodificar(endereco).localizacao();
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

    public AtendimentoCliente simularAtendimento(Long clienteId, Long restauranteId) {
        Cliente cliente = consultar(clienteId);
        Restaurante restaurante = restauranteRepository.findById(restauranteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Restaurante não encontrado"));
        Endereco destino = cliente.enderecoPrincipal();
        double distanciaDoCliente = cliente.calcularDistanciaAte(restaurante);
        double distanciaDoEndereco = destino.calcularDistanciaAte(restaurante.getEndereco());
        Rota rota = mapaGateway.calcularRota(restaurante.getEndereco().getLocalizacao(),
                destino.getLocalizacao());
        boolean atendido = politicaAreaAtendimento.atende(cliente, restaurante, destino,
                rota.distanciaKm());
        BigDecimal taxa = cliente.calcularTaxaEntregaDoRestaurante(restaurante);
        int tempo = Math.max(cliente.estimarTempoAte(restaurante), rota.duracaoMinutos());
        return new AtendimentoCliente(atendido, distanciaDoCliente, distanciaDoEndereco,
                rota.distanciaKm(), rota.regiao(), taxa, tempo);
    }

    public record AtendimentoCliente(boolean atendido, double distanciaClienteKm, double distanciaEnderecoKm,
                                     double distanciaProvedorKm, String zonaProvedor, BigDecimal taxaCliente,
                                     int tempoEstimadoMinutos) {
    }
}
