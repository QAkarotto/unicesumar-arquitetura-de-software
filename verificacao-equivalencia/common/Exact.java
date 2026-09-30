public class Exact {
    public static void main(String[] a) {
        var A = new Harness.Scen(1, -25.100, -50.150, 25, "Centro", "Ponta Grossa", "84000-000",
                -25.095, -50.160, "Centro", -25.095, -50.160, "Centro", "Ponta Grossa", "84000-000", 1, new double[]{30.0}, new int[]{2});
        var B = new Harness.Scen(2, -25.100, -50.150, 25, "Centro", "Ponta Grossa", "84000-000",
                -25.150, -50.200, "Uvaranas", -25.150, -50.200, "Uvaranas", "Ponta Grossa", "84010-100", 1, new double[]{30.0}, new int[]{2});
        var C = new Harness.Scen(3, -25.100, -50.150, 25, "Centro", "Ponta Grossa", "84000-000",
                -25.050, -50.100, "Oficinas", -25.050, -50.100, "Oficinas", "Ponta Grossa", "84000-999", 1, new double[]{30.0}, new int[]{2});
        for (var s : new Harness.Scen[]{A, B, C}) {
            System.out.println("== " + s.id());
            for (String p : Flow.run(s).split(" ; ")) System.out.println("  " + p);
        }
    }
}
