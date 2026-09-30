import br.edu.foodnow.model.*;
import java.math.BigDecimal;
import java.util.*;

public class Harness {
    public record Scen(long id, double restLat, double restLon, double raio, String restBairro, String restCidade, String restCep,
                       double priLat, double priLon, String priBairro,
                       double cliLat, double cliLon, String cliBairro, String cliCidade, String cliCep,
                       int nItens, double[] precos, int[] qtds) {}

    static final String[] BAIRROS = {"Centro", "Uvaranas", "Oficinas", "Nova Rússia", "centro"};
    static final String[] CIDADES = {"Ponta Grossa", "Ponta Grossa", "Ponta Grossa", "Castro", "ponta grossa"};
    static final String[] CEPS = {"84000-000", "84010-100", "84000-999", "84160-000", "8", null};

    public static List<Scen> scenarios(int n) {
        Random r = new Random(20260930L);
        List<Scen> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double scale = i % 4 == 0 ? 0.02 : i % 4 == 1 ? 0.08 : i % 4 == 2 ? 0.25 : 0.6;
            double restLat = -25.1 + (r.nextDouble() - .5) * 0.05;
            double restLon = -50.15 + (r.nextDouble() - .5) * 0.05;
            double cliLat = restLat + (r.nextDouble() - .5) * scale;
            double cliLon = restLon + (r.nextDouble() - .5) * scale;
            double priLat = restLat + (r.nextDouble() - .5) * scale;
            double priLon = restLon + (r.nextDouble() - .5) * scale;
            double raio = new double[]{0.5, 2, 5, 10, 25, 60}[r.nextInt(6)];
            int nIt = 1 + r.nextInt(3);
            double[] p = new double[nIt]; int[] q = new int[nIt];
            for (int k = 0; k < nIt; k++) { p[k] = 5 + Math.round(r.nextDouble() * 5000) / 100.0; q[k] = 1 + r.nextInt(4); }
            int b = r.nextInt(BAIRROS.length), c = r.nextInt(CIDADES.length), cp = r.nextInt(CEPS.length);
            int rc = r.nextInt(3);
            l.add(new Scen(i, restLat, restLon, raio, "Centro", rc == 2 ? "Castro" : "Ponta Grossa", rc == 1 ? "84010-100" : "84000-000",
                    priLat, priLon, BAIRROS[r.nextInt(BAIRROS.length)],
                    cliLat, cliLon, BAIRROS[b], CIDADES[c], CEPS[cp], nIt, p, q));
        }
        return l;
    }

    public static Endereco end(String bairro, String cidade, String cep, double lat, double lon) {
        return new Endereco("Rua X", "1", bairro, cidade, cep, new Localizacao(lat, lon));
    }

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 3000;
        for (Scen s : scenarios(n)) {
            String out;
            try { out = Flow.run(s); } catch (RuntimeException e) { out = "EXC " + e.getClass().getSimpleName() + ":" + e.getMessage(); }
            System.out.println(s.id() + " | " + out);
        }
    }
}
