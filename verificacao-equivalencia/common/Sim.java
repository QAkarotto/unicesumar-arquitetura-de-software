import br.edu.foodnow.integration.*;
import br.edu.foodnow.model.*;

/** Aritmética original de ClienteService.simularAtendimento e RestauranteService.simularEntrega. */
public class Sim {
    public static void main(String[] a) {
        FakeMapsClient maps = new FakeMapsClient();
        Restaurante restaurante = new Restaurante("R", 25, Harness.end("Centro", "Ponta Grossa", "84000-000", -25.100, -50.150));
        Cliente cliente = new Cliente("C", "c@x");
        Endereco destinoCli = Harness.end("Centro", "Ponta Grossa", "84000-000", -25.095, -50.160);
        cliente.adicionarEndereco(destinoCli);
        // atendimento
        Endereco destino = cliente.enderecoPrincipal();
        double dCli = cliente.calcularDistanciaAte(restaurante);
        double dEnd = destino.calcularDistanciaAte(restaurante.getEndereco());
        var rota = maps.calcularRota(restaurante.getEndereco().getLocalizacao(), destino.getLocalizacao());
        boolean atendido = cliente.estaDentroDaAreaDeEntrega(restaurante) && restaurante.atendeEndereco(destino) && rota.distanceKm() <= restaurante.getRaioEntregaKm();
        System.out.println("ATEND atendido=" + atendido + " dCli=" + dCli + " dEnd=" + dEnd + " dProv=" + rota.distanceKm()
                + " zona=" + rota.deliveryZone() + " taxa=" + cliente.calcularTaxaEntregaDoRestaurante(restaurante)
                + " tempo=" + Math.max(cliente.estimarTempoAte(restaurante), rota.durationMinutes()));
        // simulacao restaurante
        Endereco d = Harness.end("Centro", "Ponta Grossa", "84000-000", -25.096, -50.161);
        var r2 = maps.calcularRota(restaurante.getEndereco().getLocalizacao(), d.getLocalizacao());
        System.out.println("SIM dRest=" + restaurante.calcularDistanciaAte(d) + " dEnds=" + restaurante.getEndereco().calcularDistanciaAte(d)
                + " dProv=" + r2.distanceKm() + " regiao=" + restaurante.classificarRegiaoDeEntrega(d) + " zona=" + r2.deliveryZone()
                + " taxaRest=" + restaurante.calcularTaxaEntrega(d) + " taxaEnd=" + restaurante.getEndereco().calcularTaxaLocalAte(d)
                + " tempo=" + Math.max(restaurante.estimarTempoEntrega(d), d.estimarMinutosAte(restaurante.getEndereco())));
    }
}
