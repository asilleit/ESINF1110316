package Usei06;

import PL.BST;
import Model.Station;
import PL.AVL;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Adriano Índice por time_zone_group e country. - Para cada tzGroup,
 * manter um AVL de CountryBucket (ordenado por país) - Cada CountryBucket tem
 * um TreeSet<Station> ordenado por nome (e país como tie-break) - Validação de
 * linhas segundo o enunciado
 */
public class TimeZoneIndex {

    //Grupo -> índice interno 
    private final Map<String, GroupIndex> groups = new HashMap<>();

    public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LONGITUDE = 180.0;
    public static final double MIN_LONGITUDE = -180.0;

    // Comparador de estações: nome ASC, depois país ASC (garante total order)
    private static final Comparator<Station> STATION_BY_NAME
            = Comparator.comparing(Station::getName, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(Station::getCountry, Comparator.nullsFirst(String::compareTo));

    // Bucket de país dentro de um tzGroup. Ordena por nome do país. 
    private static final class CountryBucket implements Comparable<CountryBucket> {

        final String country;
        final TreeSet<Station> bucket = new TreeSet<>(STATION_BY_NAME);

        CountryBucket(String country) {
            this.country = country;
        }

        @Override
        public int compareTo(CountryBucket o) {
            return this.country.compareTo(o.country);
        }

        @Override
        public String toString() {
            return country + " (" + bucket.size() + ")";
        }
    }

    // Estrutura interna de um tzGroup: AVL por país e um hash para lookup 
    private static final class GroupIndex {

        final AVL<CountryBucket> countriesAVL = new AVL<>();
        final Map<String, CountryBucket> byCountry = new HashMap<>();

        int height() {
            return countriesAVL.height();
        }

        int countries() {
            return byCountry.size();
        }

        Iterable<CountryBucket> inOrderCountries() {
            return countriesAVL.inOrder();
        }
    }

    /* ========================== API ========================== */
    //Valida e insere a estação no índice.
    public void add(Station s) {
        validate(s);

        String tzg = s.getTimeZoneGroup();
        GroupIndex gi = groups.computeIfAbsent(tzg, k -> new GroupIndex());

        CountryBucket cb = gi.byCountry.get(s.getCountry());
        if (cb == null) {
            cb = new CountryBucket(s.getCountry());
            cb.bucket.add(s);
            gi.countriesAVL.insert(cb);          // ordenado por país
            gi.byCountry.put(s.getCountry(), cb);
        } else {
            cb.bucket.add(s);                    // ordenado por nome dentro do país
        }
    }

    // Todas as estações de um time zone group, por país ASC e nome ASC.
    public List<Station> byTimeZoneGroup(String tzGroup) {
        GroupIndex gi = groups.get(tzGroup);
        if (gi == null) {
            return Collections.emptyList();
        }
        List<Station> out = new ArrayList<>();
        for (CountryBucket cb : gi.inOrderCountries()) {
            out.addAll(cb.bucket);               // TreeSet já por nome
        }
        return out;
    }

    /**
     * Janela de time zone groups. Junta tudo e ordena por (country ASC, name
     * ASC). Se preferires manter “por grupos”, é só não ordenar aqui e
     * concatenar.
     */
    public List<Station> byTimeZoneWindow(List<String> tzGroups) {
        if (tzGroups == null || tzGroups.isEmpty()) {
            return Collections.emptyList();
        }
        List<Station> acc = new ArrayList<>();
        for (String g : tzGroups) {
            acc.addAll(byTimeZoneGroup(g));
        }
        // Ordena globalmente por Country ASC -> Name ASC (para output estável)
        acc.sort(Comparator.comparing(Station::getCountry).thenComparing(Station::getName));
        return acc;
    }

    // Nº de time_zone_groups distintos no índice. 
    public int size() {
        return groups.size();
    }

    //  Altura máxima dos AVLs de países (útil para diagnóstico/relatório). 
    public int height() {
        if (groups.isEmpty()) {
            return -1;
        }
        int max = -1;
        for (GroupIndex gi : groups.values()) {
            max = Math.max(max, gi.height());
        }
        return max;
    }

    /**
     * Lista de países existentes num grupo (ordenados) – opcional, mas dá
     * jeito.
     */
    public List<String> countriesInGroup(String tzGroup) {
        GroupIndex gi = groups.get(tzGroup);
        if (gi == null) {
            return Collections.emptyList();
        }
        List<String> cs = new ArrayList<>();
        for (CountryBucket cb : gi.inOrderCountries()) {
            cs.add(cb.country);
        }
        return cs;
    }

    /* ====================== validação ======================= */
    private static void validate(Station s) {
        if (s == null) {
            throw new IllegalArgumentException("Station null");
        }

        if (isBlank(s.getName())) {
            throw new IllegalArgumentException("Invalid name");
        }
        if (isBlank(s.getCountry())) {
            throw new IllegalArgumentException("Invalid country");
        }
        if (isBlank(s.getTimeZoneGroup())) {
            throw new IllegalArgumentException("Invalid tz group");
        }

        double lat = s.getLatitude();
        double lon = s.getLongitude();
        if (Double.isNaN(lat) || lat < MIN_LATITUDE || lat > MAX_LATITUDE) {
            throw new IllegalArgumentException("Invalid latitude: " + lat);
        }
        if (Double.isNaN(lon) || lon < MIN_LONGITUDE || lon > MAX_LONGITUDE) {
            throw new IllegalArgumentException("Invalid longitude: " + lon);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
