package br.edu.foodnow.localizacao;

/**
 * Rota de uma entrega depois de reconciliada.
 *
 * <p>O sistema mede a mesma distância por fórmulas diferentes e historicamente adota a maior
 * delas. {@code distanciaKm} é esse valor reconciliado e {@code rotaProvedor} preserva o que o
 * fornecedor de mapas respondeu, porque região e duração continuam vindo dele.</p>
 */
public record RotaEntrega(double distanciaKm, Rota rotaProvedor) {

    public double distanciaProvedorKm() {
        return rotaProvedor.distanciaKm();
    }

    public int duracaoProvedorMinutos() {
        return rotaProvedor.duracaoMinutos();
    }

    public String regiaoProvedor() {
        return rotaProvedor.regiao();
    }
}
