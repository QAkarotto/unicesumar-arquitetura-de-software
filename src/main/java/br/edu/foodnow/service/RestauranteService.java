package br.edu.foodnow.service;

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

    public RestauranteService(RestauranteRepository restauranteRepository, ProdutoRepository produtoRepository,
                              LocalizacaoService localizacaoService) {
        this.restauranteRepository = restauranteRepository;
        this.produtoRepository = produtoRepository;
        this.localizacaoService = localizacaoService;
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
}
