package CSV_File_Reader;

import Model.Station;
import java.io.*;
import java.util.*;

/**
 *
 * @author Adriano
 */

public class CSVReader {
    
     public static final double MAX_LATITUDE = 90.0;
    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LONGITUDE = 180.0;
    public static final double MIN_LONGITUDE = -180.0;
    
    public List<Station> readStations(String filePath) throws IOException {
        List<Station> stations = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line; int lineNo = 0;
            // header
            String header = br.readLine(); lineNo++;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) continue;
                try {
                    Station s = parse(line);
                    if (isValid(s)) stations.add(s);
                } catch (Exception e) {
                    // rejeita inválida, continua
                }
            }
        }
        return stations;
    }

    private Station parse(String line) {
        List<String> f = split(line);
        if (f.size() < 9) throw new IllegalArgumentException("The file does not contain the required columns. (9)");
        String country=f.get(0).trim();
        String tz     = cleanTZ(f.get(1));
        String tzG    = clean(f.get(2));
        String name   = clean(f.get(3));
        String latS   = clean(f.get(4));
        String lonS   = clean(f.get(5));
        String cityS  = clean(f.get(6));
        String mainS  = clean(f.get(7));
        String airS   = clean(f.get(8));

        double lat = Double.parseDouble(latS);
        double lon = Double.parseDouble(lonS);
        boolean isCity = "true".equalsIgnoreCase(cityS);
        boolean isMain = "true".equalsIgnoreCase(mainS);
        boolean isAirport = "true".equalsIgnoreCase(airS);

        return new Station(name, country, tz, tzG, lat, lon, isCity, isMain, isAirport);
    }

    private boolean isValid(Station s) {
        if (s.getName()==null || s.getName().isEmpty()) return false;
        if (s.getCountry()==null || s.getCountry().isEmpty()) return false;
        if (s.getTimeZoneGroup()==null || s.getTimeZoneGroup().isEmpty()) return false;
        if (s.getLatitude() < MIN_LATITUDE || s.getLatitude() > MAX_LATITUDE) return false;
        if (s.getLongitude() < MIN_LONGITUDE || s.getLongitude() > MAX_LONGITUDE) return false;
        return true;
    }

    private List<String> split(String line){
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes=false;
        for (int i=0;i<line.length();i++){
            char c=line.charAt(i);
            if (c=='"') inQuotes=!inQuotes;
            else if (c==',' && !inQuotes){ out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }
    private String clean(String s){
        if (s==null) return null;
        s=s.trim();
        if (s.length()>=2 && s.startsWith("\"") && s.endsWith("\"")) s=s.substring(1,s.length()-1);
        return s.trim();
    }
    private String cleanTZ(String raw){
        if (raw==null) return null;
        String s = clean(raw);
        return s.replaceAll("[^A-Za-z/_\\-]",""); // "('Europe/Paris',)" -> Europe/Paris
    }
}
