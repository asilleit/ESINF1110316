package Usei08;

import Usei07.KDTree2D;
import Model.Station;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author Adriano
 * US08 — Range queries na KD-tree com filtros opcionais.
 *
 * Região: lat ∈ [latMin, latMax], lon ∈ [lonMin, lonMax] (inclusive).
 * Filtros opcionais:
 *   - is_city (Boolean, null = ignorar)
 *   - is_main_station (Boolean, null = ignorar)
 *   - country ("PT","ES",... ou "all"/null = ignorar)
 *
 * Complexidade típica: O(log n + k) com poda (k = nº resultados).
 */
public class GeoQueries {

    private final KDTree2D kd;

    public GeoQueries(KDTree2D kd) {
        this.kd = kd;
    }

    /**
     * Devolve estações dentro do retângulo, aplicando filtros opcionais.
     * Limites inclusivos.
     */
    public List<Station> range(double latMin, double latMax,
                               double lonMin, double lonMax,
                               Boolean isCity, Boolean isMainStation,
                               String country) {

        // normalizar (caso o chamador troque limites)
        if (latMin > latMax) { double t = latMin; latMin = latMax; latMax = t; }
        if (lonMin > lonMax) { double t = lonMin; lonMin = lonMax; lonMax = t; }

        List<Station> out = new ArrayList<>();
        rangeRec(kd.getRoot(), latMin, latMax, lonMin, lonMax, isCity, isMainStation, country, out);

        // Resultado determinista — por nome (e depois país como tie-break)
        out.sort(Comparator.comparing(Station::getName)
                           .thenComparing(Station::getCountry));
        return out;
    }

    /* -------------------------- internos -------------------------- */

    private void rangeRec(KDTree2D.Node n,
                          double latMin, double latMax,
                          double lonMin, double lonMax,
                          Boolean isCity, Boolean isMainStation,
                          String country,
                          List<Station> out) {
        if (n == null) return;

        // 1) Se o ponto do nó está no retângulo, examina o bucket
        if (inRange(n.lat, latMin, latMax) && inRange(n.lon, lonMin, lonMax)) {
            for (Station s : n.bucket) {
                if (matchesFilters(s, isCity, isMainStation, country)) {
                    out.add(s);
                }
            }
        }

        // 2) Poda por eixo
        if (n.splitByLat) {
            // comparar pelo LATITUDE
            boolean goLeft  = latMin <= n.lat; // retângulo estende-se à esquerda do plano
            boolean goRight = latMax >= n.lat; // retângulo estende-se à direita do plano
            if (goLeft)  rangeRec(n.left,  latMin, latMax, lonMin, lonMax, isCity, isMainStation, country, out);
            if (goRight) rangeRec(n.right, latMin, latMax, lonMin, lonMax, isCity, isMainStation, country, out);
        } else {
            // comparar pelo LONGITUDE
            boolean goLeft  = lonMin <= n.lon;
            boolean goRight = lonMax >= n.lon;
            if (goLeft)  rangeRec(n.left,  latMin, latMax, lonMin, lonMax, isCity, isMainStation, country, out);
            if (goRight) rangeRec(n.right, latMin, latMax, lonMin, lonMax, isCity, isMainStation, country, out);
        }
    }

    private boolean inRange(double v, double a, double b) {
        return v >= a && v <= b; // inclusivo
    }

    private boolean matchesFilters(Station s,
                                   Boolean isCity,
                                   Boolean isMainStation,
                                   String country) {
        if (isCity != null && s.isCity() != isCity) return false;
        if (isMainStation != null && s.isMainStation() != isMainStation) return false;

        if (country != null) {
            String c = country.trim();
            if (!c.equalsIgnoreCase("all") && !c.isEmpty()) {
                if (!c.equalsIgnoreCase(s.getCountry())) return false;
            }
        }
        return true;
    }
}
