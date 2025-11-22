package Sprint2;

import Model.Station;
import CSV_File_Reader.CSVReader;
import Usei06.TimeZoneIndex;
import Usei07.KDTree2D;
import Usei08.GeoQueries;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Adriano
 * Main para demonstrar USEI06, USEI07 e US08.
 */
public class Main {

    private static final String CSV_FILE_PATH =
            "src/CSV_File_Reader/train_stations_europe.csv";

    public static void main(String[] args) {
        try {
            System.out.println("=== CARREGAR ESTACOES ===");
            List<Station> stations = loadStations(CSV_FILE_PATH);
            System.out.println("Total de estacoes validas: " + stations.size() + "\n");

            showFirst(stations, 5);

            // USEI06 — índices por time_zone_group
            runUSEI06(stations);

            // USEI07 — KD-tree (build balanceado + métricas)
            KDTree2D kd = runUSEI07(stations);

            // US08 — range queries + filtros (sobre a KD)
            runUS08(kd);

            System.out.println("\n=== FIM ===");

        } catch (IOException e) {
            System.err.println("ERRO: Nao foi possivel ler o CSV:");
            System.err.println("  " + CSV_FILE_PATH);
            System.err.println("Detalhes: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERRO INESPERADO: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Station> loadStations(String filePath) throws IOException {
        CSVReader reader = new CSVReader();
        return reader.readStations(filePath);
    }

    private static void showFirst(List<Station> stations, int n) {
        System.out.println("=== AMOSTRA (" + Math.min(n, stations.size()) + ") ===");
        for (int i = 0; i < Math.min(n, stations.size()); i++) {
            Station s = stations.get(i);
            System.out.println("  - " + s);
        }
        System.out.println();
    }

    /* ----------------------------- USEI06 ----------------------------- */

    private static void runUSEI06(List<Station> stations) {
        System.out.println("=== USEI06 -- Time-Zone Index & consultas por lat/lon (via KD) ===");

        TimeZoneIndex tzIndex = new TimeZoneIndex();
        for (Station s : stations) tzIndex.add(s);

        // 1) todas as estações do grupo CET (por país ascendente e nome ascendente)
        var cet = tzIndex.byTimeZoneGroup("CET");
        System.out.println("CET -> " + Math.min(5, cet.size()) + " primeiras: " + cet.stream().limit(5).toList());

        // 2) janela de grupos ['CET','WET/GMT']
        var window = tzIndex.byTimeZoneWindow(Arrays.asList("CET", "WET/GMT"));
        System.out.println("[CET, WET/GMT] -> " + Math.min(5, window.size()) + " primeiras: " + window.stream().limit(5).toList());

        // 3) “Busca por latitude/longitude” usando KD + GeoQueries com limites iguais
        KDTree2D kdTmp = new KDTree2D();
        kdTmp.buildBalanced(stations);
        GeoQueries gqTmp = new GeoQueries(kdTmp);

        // “mesma latitude” = retângulo [lat,lat] × [-180,180]
        var sameLat = gqTmp.range(44.35, 44.35, -180.0, 180.0, null, null, "all");
        System.out.println("lat = 44.35 -> " + sameLat.size() + " estacoes (ordenadas por nome)");

        // “mesma longitude” = retângulo [-90,90] × [lon,lon]
        var sameLon = gqTmp.range(-90.0, 90.0, 6.35, 6.35, null, null, "all");
        System.out.println("lon = 6.35 -> " + sameLon.size() + " estacoes (ordenadas por nome)");

        System.out.println("Complexidade USEI06:");
        System.out.println("- TZ group: hash avg O(1) + ordenacao por pais em O(P log P)");
        System.out.println("- Lat/Lon via KD (igualdade): range query com limites iguais -> poda + visita local\n");
    }

    /* ----------------------------- USEI07 ----------------------------- */

    private static KDTree2D runUSEI07(List<Station> stations) {
        System.out.println("=== USEI07 -- KD-tree (bulk build balanceado) ===");
        KDTree2D kd = new KDTree2D();

        long t0 = System.nanoTime();
        kd.buildBalanced(stations);
        long t1 = System.nanoTime();

        System.out.println("KD size (nos/coords distintas): " + kd.size());
        System.out.println("KD height: " + kd.height());
        System.out.println("Distinct bucket sizes: " + kd.distinctBucketSizes());
        System.out.printf("Build time: ~%.1f ms%n", (t1 - t0) / 1e6);

        System.out.println("Complexidade USEI07:");
        System.out.println("- Bulk build: O(n log n) (ordenar por eixos recursivamente)");
        System.out.println("- Altura esperada: ~O(log n) com mediana\n");

        return kd;
    }

    /* ----------------------------- US08 ----------------------------- */

    private static void runUS08(KDTree2D kd) {
        System.out.println("=== US08 -- Range queries com filtros ===");
        GeoQueries gq = new GeoQueries(kd);

        var q1 = gq.range(43.0, 45.0, 5.0, 7.5, null, null, "FR");
        System.out.println("FR 43-45 lat, 5-7.5 lon -> " + q1.size() + " estacoes; exemplos: " + q1.stream().limit(5).toList());

        var q2 = gq.range(38.4, 38.9, -9.4, -8.9, null, true, "PT");
        System.out.println("PT main 38.4-38.9 lat, -9.4--8.9 lon -> " + q2.size() + " estacoes; exemplos: " + q2.stream().limit(5).toList());

        var q3 = gq.range(41.3, 41.5, 2.0, 2.3, true, null, "ES");
        System.out.println("ES city 41.3-41.5 lat, 2.0-2.3 lon -> " + q3.size() + " estacoes; exemplos: " + q3.stream().limit(5).toList());

        var q4 = gq.range(50.0, 52.0, 4.0, 9.0, null, null, "all");
        System.out.println("ALL 50-52 lat, 4-9 lon -> " + q4.size());

        System.out.println("Complexidade US08:");
        System.out.println("- Range KD: O(log n + k) tipico (k = resultados), com poda por eixo\n");
    }
}
