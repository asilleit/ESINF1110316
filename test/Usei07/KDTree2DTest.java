package Usei07;

import Usei07.KDTree2D;
import Model.Station;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Testes para USEI07 — KDTree2D (k=2).
 */
public class KDTree2DTest {

    private List<Station> sample;

    @Before
    public void setUp() {
        // Dataset pequeno e estável, com buckets (coordenadas repetidas)
        sample = Arrays.asList(
            // bucket 1: (38.73, -9.12)
            new Station("Lisboa A", "PT","Europe/Lisbon","WET/GMT", 38.73, -9.12, false, true,  false),
            new Station("Lisboa B", "PT","Europe/Lisbon","WET/GMT", 38.73, -9.12, false, false, false),

            // bucket 2: (41.38,  2.14)
            new Station("Barcelona Passeig", "ES","Europe/Madrid","CET", 41.38, 2.14, true,  false, false),
            new Station("Barcelona Sants",   "ES","Europe/Madrid","CET", 41.38, 2.14, true,  true,  false),

            // buckets unitários
            new Station("Madrid Atocha", "ES","Europe/Madrid","CET", 40.40, -3.69, true, true, false),
            new Station("Paris Lyon",    "FR","Europe/Paris", "CET", 48.84,  2.37, false,true, false),
            new Station("Porto Campanhã","PT","Europe/Lisbon","WET/GMT", 41.15, -8.60, false,true,false)
        );
    }

    @Test
    public void testBuildBalanced_Empty() {
        KDTree2D kd = new KDTree2D();
        kd.buildBalanced(Collections.emptyList());

        assertEquals(0, kd.size());
        assertEquals(-1, kd.height());
        assertNull(kd.getRoot());
        assertTrue(kd.distinctBucketSizes().isEmpty());
    }

    @Test
    public void testBuildBalanced_SizeAndHeight() {
        KDTree2D kd = new KDTree2D();
        kd.buildBalanced(sample);

        // Distintas coordenadas: (38.73,-9.12), (41.38,2.14), (40.40,-3.69),
        // (48.84,2.37), (41.15,-8.60) => 5 nós
        assertEquals(5, kd.size());

        // Árvores muito pequenas: altura razoável (não frágil)
        assertTrue(kd.height() >= 2 && kd.height() <= 4);
        assertNotNull(kd.getRoot());
    }

    @Test
    public void testBucketsAndOrdering() {
        KDTree2D kd = new KDTree2D();
        kd.buildBalanced(sample);

        // bucket (38.73, -9.12) => Lisboa A, Lisboa B (ordem por nome)
        KDTree2D.Node nLisboa = find(kd.getRoot(), 38.73, -9.12);
        assertNotNull(nLisboa);
        assertEquals(2, nLisboa.bucket.size());
        assertEquals("Lisboa A", nLisboa.bucket.get(0).getName());
        assertEquals("Lisboa B", nLisboa.bucket.get(1).getName());

        // bucket (41.38, 2.14) => Barcelona Passeig, Barcelona Sants (ordem por nome)
        KDTree2D.Node nBcn = find(kd.getRoot(), 41.38, 2.14);
        assertNotNull(nBcn);
        assertEquals(2, nBcn.bucket.size());
        assertEquals("Barcelona Passeig", nBcn.bucket.get(0).getName());
        assertEquals("Barcelona Sants",   nBcn.bucket.get(1).getName());
    }

    @Test
    public void testDistinctBucketSizes() {
        KDTree2D kd = new KDTree2D();
        kd.buildBalanced(sample);

        // Temos dois buckets de tamanho 2 e três de tamanho 1 => {1,2}
        List<Integer> sizes = kd.distinctBucketSizes();
        assertEquals(Arrays.asList(1, 2), sizes);
    }

    @Test
    public void testSplitAxisAlternation() {
        KDTree2D kd = new KDTree2D();
        kd.buildBalanced(sample);

        KDTree2D.Node r = kd.getRoot();
        assertNotNull(r);
        assertTrue("Root deve dividir por latitude", r.splitByLat);

        // filhos, se existirem, alternam
        if (r.left != null)  assertFalse("Filho esquerdo alterna para longitude", r.left.splitByLat);
        if (r.right != null) assertFalse("Filho direito alterna para longitude",  r.right.splitByLat);

        // e volta a alternar nos netos
        if (r.left != null && r.left.left != null)  assertTrue(r.left.left.splitByLat);
        if (r.left != null && r.left.right != null) assertTrue(r.left.right.splitByLat);
        if (r.right != null && r.right.left != null)  assertTrue(r.right.left.splitByLat);
        if (r.right != null && r.right.right != null) assertTrue(r.right.right.splitByLat);
    }

    @Test
    public void testDeterministicBuild() {
        KDTree2D kd1 = new KDTree2D();
        kd1.buildBalanced(sample);

        // baralhar e voltar a construir
        List<Station> shuffled = new ArrayList<>(sample);
        Collections.shuffle(shuffled, new Random(1234));
        KDTree2D kd2 = new KDTree2D();
        kd2.buildBalanced(shuffled);

        // A “assinatura” (pre-order dos nós com (lat,lon, primeiro-nome-do-bucket)) deve coincidir
        String sig1 = signature(kd1.getRoot());
        String sig2 = signature(kd2.getRoot());
        assertEquals(sig1, sig2);
    }

    /* ---------------------- helpers de teste ---------------------- */

    private KDTree2D.Node find(KDTree2D.Node n, double lat, double lon) {
        // procura por (lat,lon) navegando conforme o eixo do nó
        KDTree2D.Node cur = n;
        while (cur != null) {
            if (Double.compare(lat, cur.lat) == 0 && Double.compare(lon, cur.lon) == 0) {
                return cur;
            }
            if (cur.splitByLat) {
                cur = (lat < cur.lat) ? cur.left : cur.right;
            } else {
                cur = (lon < cur.lon) ? cur.left : cur.right;
            }
        }
        return null;
    }

    private String signature(KDTree2D.Node n) {
        StringBuilder sb = new StringBuilder();
        preSig(n, sb);
        return sb.toString();
    }

    private void preSig(KDTree2D.Node n, StringBuilder sb) {
        if (n == null) { sb.append("#"); return; }
        String firstName = n.bucket.isEmpty() ? "-" : n.bucket.get(0).getName();
        sb.append('(').append(n.lat).append(',').append(n.lon).append(',').append(firstName).append(',').append(n.splitByLat).append(')');
        preSig(n.left, sb);
        preSig(n.right, sb);
    }
}
