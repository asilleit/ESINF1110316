package Model;

import java.util.Objects;
/**
 * @author Adriano
 */

public class Station implements Comparable<Station> {
    private final String name;
    private final String country;
    private final String timeZone;       // Europe/Paris, ...
    private final String timeZoneGroup;  // CET, WET/GMT, ...
    private final double lat;
    private final double lon;
    private final boolean isCity;
    private final boolean isMainStation;
    private final boolean isAirport;

    public Station(String name, String country, String timeZone, String timeZoneGroup,
                   double lat, double lon, boolean isCity, boolean isMainStation, boolean isAirport) {
        this.name = name;
        this.country = country;
        this.timeZone = timeZone;
        this.timeZoneGroup = timeZoneGroup;
        this.lat = lat;
        this.lon = lon;
        this.isCity = isCity;
        this.isMainStation = isMainStation;
        this.isAirport = isAirport;
    }

    public Station(String name, String country, String timeZoneGroup,
                   double lat, double lon, boolean isCity, boolean isMainStation) {
        this(name, country, null, timeZoneGroup, lat, lon, isCity, isMainStation, false);
    }

    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getTimeZone() { return timeZone; }
    public String getTimeZoneGroup() { return timeZoneGroup; }
    public double getLatitude() { return lat; }
    public double getLongitude() { return lon; }
    public boolean isCity() { return isCity; }
    public boolean isMainStation() { return isMainStation; }
    public boolean isAirport() { return isAirport; }

    /** Ordenação por nome (para listas dentro dos buckets). */
    @Override public int compareTo(Station o) { return this.name.compareTo(o.name); }

    @Override public String toString() {
        return name + " [" + country + "] (" + lat + "," + lon + ") " + timeZoneGroup;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Station)) return false;
        Station s = (Station) o;
        return Double.compare(s.lat, lat) == 0 && Double.compare(s.lon, lon) == 0 &&
               Objects.equals(name, s.name) && Objects.equals(country, s.country);
    }
    @Override public int hashCode() { return Objects.hash(name, country, lat, lon); }
}
