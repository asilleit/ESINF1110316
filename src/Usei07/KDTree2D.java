package Usei07;

import Model.Station;
import PL.BST;
import java.util.*;

/**
 * @author Adriano
 * KD-tree k=2 em (lat,lon) com bulk build balanceado.
 * Usa PL.BST para ordenar determinística/estavelmente por eixo a cada nível.
 */
public class KDTree2D {

    public static final class Node {
        public final double lat, lon;
        public final List<Station> bucket;   // estações nestas coords (ordenadas por nome)
        public boolean splitByLat;
        public Node left, right;

        Node(double lat, double lon, List<Station> bucket, boolean splitByLat) {
            this.lat = lat;
            this.lon = lon;
            this.bucket = bucket;
            this.splitByLat = splitByLat;
        }
    }

    private Node root;
    private int size;    // nº de nós (pares (lat,lon) distintos)
    private int height;  // altura (árvore vazia = -1)

    public Node getRoot() { return root; }
    public int size()     { return size; }
    public int height()   { return height; }

    /** Chave para ordenar por eixo usando PL.BST */
    private static final class AxisKey implements Comparable<AxisKey> {
        final Node n;
        final boolean byLat;

        AxisKey(Node n, boolean byLat) { this.n = n; this.byLat = byLat; }

        @Override public int compareTo(AxisKey o) {
            int cmp = byLat ? Double.compare(n.lat, o.n.lat)
                            : Double.compare(n.lon, o.n.lon);
            if (cmp != 0) return cmp;
            // tie-breakers determinísticos:
            cmp = Double.compare(n.lon, o.n.lon);
            if (cmp != 0) return cmp;
            return n.bucket.get(0).getName().compareTo(o.n.bucket.get(0).getName());
        }
    }

    public void buildBalanced(List<Station> stations) {
        if (stations == null || stations.isEmpty()) {
            root = null; size = 0; height = -1; return;
        }

        // 1) agrupar por (lat,lon) -> buckets ordenados por nome (Station deve ser Comparable por nome)
        Map<String, List<Station>> groups = new HashMap<>();
        for (Station s : stations) {
            String key = s.getLatitude() + "#" + s.getLongitude();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        List<Node> points = new ArrayList<>(groups.size());
        for (List<Station> b : groups.values()) {
            b.sort(Comparator.naturalOrder());             // por nome
            Station any = b.get(0);
            points.add(new Node(any.getLatitude(), any.getLongitude(), b, true));
        }

        // 2) bulk build recursivo usando BST para ordenar por eixo
        size = points.size();
        root = buildRec(points, true);                     // começa a cortar por latitude
        height = computeHeight(root);
    }

    private Node buildRec(List<Node> pts, boolean splitByLat) {
        if (pts.isEmpty()) return null;

        // *** ORDENAR COM A TUA BST ***
        BST<AxisKey> sorter = new BST<>();
        List<AxisKey> axisKeys = new ArrayList<>(pts.size());
        for (Node n : pts) axisKeys.add(new AxisKey(n, splitByLat));
        for (AxisKey k : axisKeys) sorter.insert(k);

        List<AxisKey> ordered = new ArrayList<>();
        for (AxisKey k : sorter.inOrder()) ordered.add(k);

        int mid = ordered.size() / 2;
        Node n = ordered.get(mid).n;
        n.splitByLat = splitByLat;

        // sublistas esquerda/direita (em ordem) reaproveitando os AxisKey
        List<Node> left  = new ArrayList<>(mid);
        for (int i = 0; i < mid; i++) left.add(ordered.get(i).n);
        List<Node> right = new ArrayList<>(ordered.size() - mid - 1);
        for (int i = mid + 1; i < ordered.size(); i++) right.add(ordered.get(i).n);

        n.left  = buildRec(left,  !splitByLat);
        n.right = buildRec(right, !splitByLat);
        return n;
    }

    private int computeHeight(Node n) {
        if (n == null) return -1;
        return 1 + Math.max(computeHeight(n.left), computeHeight(n.right));
    }

    public List<Integer> distinctBucketSizes() {
        Set<Integer> sizes = new TreeSet<>();
        collectBucketSizes(root, sizes);
        return new ArrayList<>(sizes);
    }
    private void collectBucketSizes(Node n, Set<Integer> sizes) {
        if (n == null) return;
        sizes.add(n.bucket == null ? 0 : n.bucket.size());
        collectBucketSizes(n.left, sizes);
        collectBucketSizes(n.right, sizes);
    }
}