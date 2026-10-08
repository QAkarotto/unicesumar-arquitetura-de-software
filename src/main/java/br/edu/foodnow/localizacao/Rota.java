package br.edu.foodnow.localizacao;

/**
 * Resultado de rota já traduzido para o vocabulário interno do FoodNow.
 *
 * <p>Nenhum campo aqui carrega nome, formato ou unidade de fornecedor. É este objeto que a futura
 * regra de trajeto, região e horário deve enriquecer, e não os records do cliente de mapas.</p>
 */
public record Rota(double distanciaKm, int duracaoMinutos, String regiao) {
}
