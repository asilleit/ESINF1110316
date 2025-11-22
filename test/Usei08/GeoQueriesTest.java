package Usei08;

import Usei08.GeoQueries;
import Usei07.KDTree2D;
import Model.Station;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Testes para US08 — GeoQueries (range queries com filtros).
 */
public class GeoQueriesTest {

    private KDTree2D kd;
    private GeoQueries gq;

    @Before
    public void setUp() {
        List<Station> st = Arrays.asList(
            new Station("Lisboa Oriente",        "PT","Europe/Lisbon","WET/GMT", 38.77, -9.10, false, true,  false),
            new Station("Lisboa Sta Apolónia",   "PT","Europe/Lisbon","WET/GMT", 38.73, -9.12, false, false, false),
            new Station("Barcelona Sants",       "ES","Europe/Madrid","CET",     41.38,  2.14,  true,  true,  false),
            new Station("Madrid Atocha",         "ES","Europe/Madrid","CET",     40.40, -3.69,  true,  true,  false),
            new Station("Paris Lyon",            "FR","Europe/Paris", "CET",     48.84,  2.37,  false, true,  false),
            // ponto de fronteira para testar inclusão nos limites
            new Station("Verona Porta Nuova",    "IT","Europe/Rome",  "CET",     45.00, 10.00,  false, false, false)
        );
        kd = new KDTree2D();
        kd.buildBalanced(st);
        gq = new GeoQueries(kd);
    }

    @Test
    public void testRangeLisboaMainStationsPT() {
        // região Lisboa; só estações principais (PT)
        List<Station> res = gq.range(38.4, 38.9, -9.4, -8.9,
                                     null, true, "PT");
        assertEquals(1, res.size());
        assertEquals("Lisboa Oriente", res.get(0).getName());
    }

    @Test
    public void testRangeBarcelonaOnlyCityES() {
        // região Barcelona; só city = true (ES)
        List<Station> res = gq.range(41.3, 41.5, 2.0, 2.3,
                                     true, null, "ES");
        assertEquals(1, res.size());
        assertEquals("Barcelona Sants", res.get(0).getName());
    }

    @Test
    public void testBoundsAreInclusive() {
        // ponto exatamente no canto (45.00, 10.00) — deve ser incluído
        List<Station> res = gq.range(45.0, 45.0, 10.0, 10.0,
                                     null, null, "IT");
        assertEquals(1, res.size());
        assertEquals("Verona Porta Nuova", res.get(0).getName());
    }

    @Test
    public void testDeterministicOrderingByName() {
        // Janela que apanha 2+ estações; a ordem deve ser por nome
        List<Station> res = gq.range(38.6, 41.5, -9.5, 2.5,
                                     null, null, "all");

        // Deve conter (pelo menos) Lisboa Oriente, Lisboa Sta Apolónia, Barcelona Sants
        assertTrue(res.stream().anyMatch(s -> s.getName().equals("Lisboa Oriente")));
        assertTrue(res.stream().anyMatch(s -> s.getName().equals("Lisboa Sta Apolónia")));
        assertTrue(res.stream().anyMatch(s -> s.getName().equals("Barcelona Sants")));

        // Ordem lexicográfica por nome
        int idxBarcelona = indexOf(res, "Barcelona Sants");
        int idxLisboaOri = indexOf(res, "Lisboa Oriente");
        int idxLisboaApo = indexOf(res, "Lisboa Sta Apolónia");

        assertTrue(idxBarcelona < idxLisboaOri);
        assertTrue(idxLisboaOri < idxLisboaApo);

    }

    private int indexOf(List<Station> list, String name) {
        for (int i = 0; i < list.size(); i++) if (list.get(i).getName().equals(name)) return i;
        return -1;
    }
}
