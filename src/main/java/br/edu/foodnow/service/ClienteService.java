package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeMapsClient;
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
    private final LocalizacaoService localizacaoService;
    private final FakeMapsClient mapsClient;

    public ClienteService(ClienteRepository clienteRepository, RestauranteRepository restauranteRepository,
                          LocalizacaoService localizacaoService, FakeMapsClient mapsClient) {
        this.clienteRepository = clienteRepository;
        this.restauranteRepository = restauranteRepository;
        this.localizacaoService = localizacaoService;
        this.mapsClient = mapsClient;
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
        FakeMapsClient.MapCoordinates respostaDireta = mapsClient.buscarCoordenadas(endereco);
        if ("APPROXIMATE".equals(respostaDireta.precision())) {
            coordenadas = new Localizacao(Double.parseDouble(respostaDireta.lat()),
                    Double.parseDouble(respostaDireta.lng()));
        }
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
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(restaurante.getEndereco().getLocalizacao(),
                destino.getLocalizacao());
        boolean atendido = cliente.estaDentroDaAreaDeEntrega(restaurante)
                && restaurante.atendeEndereco(destino)
                && rota.distanceKm() <= restaurante.getRaioEntregaKm();
        BigDecimal taxa = cliente.calcularTaxaEntregaDoRestaurante(restaurante);
        int tempo = Math.max(cliente.estimarTempoAte(restaurante), rota.durationMinutes());
        return new AtendimentoCliente(atendido, distanciaDoCliente, distanciaDoEndereco,
                rota.distanceKm(), rota.deliveryZone(), taxa, tempo);
    }

    public record AtendimentoCliente(boolean atendido, double distanciaClienteKm, double distanciaEnderecoKm,
                                     double distanciaProvedorKm, String zonaProvedor, BigDecimal taxaCliente,
                                     int tempoEstimadoMinutos) {
    }
}
