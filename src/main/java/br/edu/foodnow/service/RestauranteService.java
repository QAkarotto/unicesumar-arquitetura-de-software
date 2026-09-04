package br.edu.foodnow.service;

import br.edu.foodnow.integration.FakeMapsClient;
import br.edu.foodnow.model.Endereco;
import br.edu.foodnow.model.Localizacao;
import br.edu.foodnow.model.Produto;
import br.edu.foodnow.model.Restaurante;
import br.edu.foodnow.repository.ProdutoRepository;
import br.edu.foodnow.repository.RestauranteRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RestauranteService {
    private final RestauranteRepository restauranteRepository;
    private final ProdutoRepository produtoRepository;
    private final LocalizacaoService localizacaoService;
    private final FakeMapsClient mapsClient;

    public RestauranteService(RestauranteRepository restauranteRepository, ProdutoRepository produtoRepository,
                              LocalizacaoService localizacaoService, FakeMapsClient mapsClient) {
        this.restauranteRepository = restauranteRepository;
        this.produtoRepository = produtoRepository;
        this.localizacaoService = localizacaoService;
        this.mapsClient = mapsClient;
    }

    public Restaurante cadastrar(String nome, double raioEntregaKm, Endereco endereco) {
        Localizacao coordenadas = localizacaoService.buscarCoordenadas(endereco);
        Endereco localizado = new Endereco(endereco.getLogradouro(), endereco.getNumero(), endereco.getBairro(),
                endereco.getCidade(), endereco.getCep(), coordenadas);
        return restauranteRepository.save(new Restaurante(nome, raioEntregaKm, localizado));
    }

    public Restaurante consultar(Long id) {
        return restauranteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Restaurante não encontrado"));
    }

    public Produto cadastrarProduto(Long restauranteId, String nome, BigDecimal preco, boolean disponivel) {
        Restaurante restaurante = consultar(restauranteId);
        return produtoRepository.save(new Produto(nome, preco, disponivel, restaurante));
    }

    public Produto consultarProduto(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado"));
    }

    public Produto alterarDisponibilidade(Long id, boolean disponivel) {
        Produto produto = consultarProduto(id);
        produto.alterarDisponibilidade(disponivel);
        return produtoRepository.save(produto);
    }

    public double calcularDistanciaAte(Long restauranteId, Endereco endereco) {
        Restaurante restaurante = consultar(restauranteId);
        return restaurante.calcularDistanciaAte(endereco);
    }

    public SimulacaoRestaurante simularEntrega(Long restauranteId, Endereco destino) {
        Restaurante restaurante = consultar(restauranteId);
        FakeMapsClient.RouteResult rota = mapsClient.calcularRota(restaurante.getEndereco().getLocalizacao(),
                destino.getLocalizacao());
        double distanciaDaEntidade = restaurante.calcularDistanciaAte(destino);
        double distanciaDosEnderecos = restaurante.getEndereco().calcularDistanciaAte(destino);
        BigDecimal taxaDoRestaurante = restaurante.calcularTaxaEntrega(destino);
        BigDecimal taxaDoEndereco = restaurante.getEndereco().calcularTaxaLocalAte(destino);
        int tempo = Math.max(restaurante.estimarTempoEntrega(destino), destino.estimarMinutosAte(
                restaurante.getEndereco()));
        return new SimulacaoRestaurante(distanciaDaEntidade, distanciaDosEnderecos, rota.distanceKm(),
                restaurante.classificarRegiaoDeEntrega(destino), rota.deliveryZone(),
                taxaDoRestaurante, taxaDoEndereco, tempo);
    }

    public record SimulacaoRestaurante(double distanciaRestauranteKm, double distanciaEnderecosKm,
                                       double distanciaProvedorKm, String regiaoRestaurante,
                                       String zonaProvedor, BigDecimal taxaRestaurante,
                                       BigDecimal taxaEndereco, int tempoEstimadoMinutos) {
    }
}
