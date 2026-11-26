package br.edu.foodnow.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TaxaEntregaUtil {
    private TaxaEntregaUtil() {
    }

    public static BigDecimal calcular(double distanciaKm) {
        BigDecimal base = distanciaKm <= 3.0 ? BigDecimal.valueOf(4.50) : BigDecimal.valueOf(6.00);
        return base.add(BigDecimal.valueOf(distanciaKm).multiply(BigDecimal.valueOf(1.20)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
