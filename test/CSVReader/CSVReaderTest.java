package CSVReader;

import CSV_File_Reader.CSVReader;
import Model.Station;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Testes para CSVReader:
 * - Ignora header
 * - Faz parse de linhas com campos entre aspas e vírgulas
 * - Continua ao encontrar linhas inválidas (sem lançar), contando só as válidas
 */
public class CSVReaderTest {

    /** Cria um ficheiro temporário com o conteúdo dado e devolve o File */
    private File mkTempCsv(String content) throws Exception {
        File tmp = File.createTempFile("stations_test_", ".csv");
        tmp.deleteOnExit();
        try (PrintWriter pw = new PrintWriter(new FileWriter(tmp))) {
            pw.print(content);
        }
        return tmp;
    }

    @Test
    public void testReadStations_SkipsHeaderAndParsesQuotes() throws Exception {
        // Header + 3 linhas válidas (com aspas/virgulas) e 1 linha vazia
        String csv =
            "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport\n" +
            "FR,\"('Europe/Paris',)\",CET,Chateau-Arnoux-St-Auban,44.08179,6.001625,True,False,False\n" +
            "PT,\"('Europe/Lisbon',)\",WET/GMT,\"Lisboa, Oriente\",38.772, -9.101,False,True,False\n" +
            "ES,\"('Europe/Madrid',)\",CET,Barcelona Sants,41.380,2.140,True,True,False\n" +
            "\n";

        File file = mkTempCsv(csv);

        CSVReader reader = new CSVReader();
        List<Station> stations = reader.readStations(file.getAbsolutePath());

        // Header ignorado → 3 estações
        assertEquals(3, stations.size());

        // Verificações de conteúdo
        Station s0 = stations.get(0);
        assertEquals("Chateau-Arnoux-St-Auban", s0.getName());
        assertEquals("FR", s0.getCountry());
        assertEquals("CET", s0.getTimeZoneGroup());
        assertEquals(44.08179, s0.getLatitude(), 1e-6);
        assertEquals(6.001625, s0.getLongitude(), 1e-6);
        assertTrue(s0.isCity());
        assertFalse(s0.isMainStation());

        // A linha com vírgula no nome ("Lisboa, Oriente") tem de ser bem parseada
        Station s1 = stations.get(1);
        assertEquals("Lisboa, Oriente", s1.getName());
        assertEquals("PT", s1.getCountry());
        assertEquals("WET/GMT", s1.getTimeZoneGroup());
        assertFalse(s1.isCity());
        assertTrue(s1.isMainStation());
        assertEquals(38.772, s1.getLatitude(), 1e-6);
        assertEquals(-9.101, s1.getLongitude(), 1e-6);
    }

    @Test
    public void testReadStations_ContinuesOnInvalidRows() throws Exception {
        // 2 válidas + 3 inválidas (lat fora de range; nome vazio; latitude não numérica)
        String csv =
            "country,time_zone,time_zone_group,station,latitude,longitude,is_city,is_main_station,is_airport\n" +
            "PT,\"('Europe/Lisbon',)\",WET/GMT,Lisboa Sta Apolonia,38.73,-9.12,False,False,False\n" + // válida
            "XX,\"('Europe/X',)\",CET,NomeVazio,, -1.0,False,False,False\n" +                           // inválida (lat vazia)
            "ES,\"('Europe/Madrid',)\",CET,Madrid Atocha,91.0,-3.69,True,True,False\n" +               // inválida (lat > 90)
            "FR,\"('Europe/Paris',)\",CET,Paris Lyon,48.84,abc,False,True,False\n" +                    // inválida (lon não numérica)
            "ES,\"('Europe/Madrid',)\",CET,Barcelona Sants,41.38,2.14,True,True,False\n";               // válida

        File file = mkTempCsv(csv);

        CSVReader reader = new CSVReader();
        List<Station> stations = reader.readStations(file.getAbsolutePath());

        // Só entram as 2 válidas
        assertEquals(2, stations.size());
        assertTrue(stations.stream().anyMatch(s -> s.getName().equals("Lisboa Sta Apolonia")));
        assertTrue(stations.stream().anyMatch(s -> s.getName().equals("Barcelona Sants")));
    }
}
