package Usei06;

import Usei06.TimeZoneIndex;
import Model.Station;
import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Testes para USEI06 — TimeZoneIndex (índice por time_zone_group e country).
 */
public class TimeZoneIndexTest {

    private TimeZoneIndex tz;

    @Before
    public void setUp() {
        tz = new TimeZoneIndex();
    }

    @Test
    public void testAddAndByTimeZoneGroup() {
        // CET (FR, ES)
        tz.add(new Station("Paris Lyon", "FR", "Europe/Paris", "CET", 48.84, 2.37, false, true, false));
        tz.add(new Station("Paris Nord", "FR", "Europe/Paris", "CET", 48.88, 2.36, false, true, false));
        tz.add(new Station("Barcelona Sants", "ES", "Europe/Madrid", "CET", 41.38, 2.14, true, true, false));
        tz.add(new Station("Madrid Atocha", "ES", "Europe/Madrid", "CET", 40.40, -3.69, true, true, false));

        // WET/GMT (PT)
        tz.add(new Station("Lisboa Oriente", "PT", "Europe/Lisbon", "WET/GMT", 38.77, -9.10, false, true, false));
        tz.add(new Station("Lisboa Sta Apolónia", "PT", "Europe/Lisbon", "WET/GMT", 38.73, -9.12, false, false, false));

        // CET → FR + ES; dentro de FR: Paris Lyon < Paris Nord
        List<Station> cet = tz.byTimeZoneGroup("CET");
        assertEquals(4, cet.size());
        assertEquals("ES", cet.get(0).getCountry());
        assertEquals("Barcelona Sants", cet.get(0).getName());
        assertEquals("FR", cet.get(2).getCountry());
        assertEquals("Paris Lyon", cet.get(2).getName());
        assertEquals("Paris Nord", cet.get(3).getName());

        // WET/GMT → PT; ordenado por nome
        List<Station> wet = tz.byTimeZoneGroup("WET/GMT");
        assertEquals(2, wet.size());
        assertEquals("Lisboa Oriente", wet.get(0).getName());
        assertEquals("Lisboa Sta Apolónia", wet.get(1).getName());
    }

    @Test
    public void testByTimeZoneWindow() {
        tz.add(new Station("Paris Lyon", "FR", "Europe/Paris", "CET", 48.84, 2.37, false, true, false));
        tz.add(new Station("Lisboa Oriente", "PT", "Europe/Lisbon", "WET/GMT", 38.77, -9.10, false, true, false));

        List<Station> win = tz.byTimeZoneWindow(Arrays.asList("WET/GMT", "CET"));
        // Ordenado globalmente: country asc (FR, PT), name asc
        assertEquals(2, win.size());
        assertEquals("FR", win.get(0).getCountry());
        assertEquals("Paris Lyon", win.get(0).getName());
        assertEquals("PT", win.get(1).getCountry());
        assertEquals("Lisboa Oriente", win.get(1).getName());
    }

    @Test
    public void testSizeAndHeight() {
        tz.add(new Station("A", "PT", "Europe/Lisbon", "WET/GMT", 10, 10, false, false, false));
        tz.add(new Station("B", "PT", "Europe/Lisbon", "WET/GMT", 10, 11, false, false, false));
        tz.add(new Station("C", "ES", "Europe/Madrid", "CET", 40, 3, false, false, false));
        assertEquals(2, tz.size());      // dois grupos distintos
        assertTrue(tz.height() >= 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidation_InvalidLatitude() {
        tz.add(new Station("A", "PT", "Europe/Lisbon", "WET/GMT", 999, 0, false, false, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidation_EmptyCountry() {
        tz.add(new Station("A", "", "Europe/Lisbon", "WET/GMT", 40, 0, false, false, false));
    }

    @Test
    public void testCountriesInGroup() {
        tz.add(new Station("Paris Lyon", "FR", "Europe/Paris", "CET", 48.84, 2.37, false, true, false));
        tz.add(new Station("Madrid Atocha", "ES", "Europe/Madrid", "CET", 40.40, -3.69, true, true, false));
        List<String> countries = tz.countriesInGroup("CET");
        assertEquals(Arrays.asList("ES", "FR"), countries);
    }
}