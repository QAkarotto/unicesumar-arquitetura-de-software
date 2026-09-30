package br.edu.foodnow.geo;

/**
 * Resultado de uma consulta de rota, expresso no vocabulário do domínio FoodNow
 * (e não no vocabulário de um provedor de mapas específico).
 *
 * @param distanciaKm     distância do trajeto, em km
 * @param duracaoMinutos  duração estimada do trajeto, em minutos
 * @param zona            zona de entrega informada pelo provedor (ex.: CENTRAL, EXPANDIDA)
 */
public record Rota(double distanciaKm, int duracaoMinutos, String zona) {
}
