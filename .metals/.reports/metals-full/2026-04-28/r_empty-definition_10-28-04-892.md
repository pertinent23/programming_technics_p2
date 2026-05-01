error id: file://<WORKSPACE>/app/src/main/java/os_p2/engine/ImageSimilarityEngine.java:VirtualFileSystem#
file://<WORKSPACE>/app/src/main/java/os_p2/engine/ImageSimilarityEngine.java
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 917
uri: file://<WORKSPACE>/app/src/main/java/os_p2/engine/ImageSimilarityEngine.java
text:
```scala
package os_p2.engine;

import java.util.List;
import java.util.stream.Stream;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Design Pattern : Strategy.
 * Moteur spécialisé pour les images via OpenCV (Photography Clients)[cite: 17, 70].
 */
public class ImageSimilarityEngine implements DeduplicationEngine {

    private static final double SIMILARITY_THRESHOLD = 0.90; // Exigence du brief [cite: 165]

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[OpenCV] ERREUR : Librairie native introuvable.");
        }
    }

    @Override
    public Stream<List<String>> scan(VirtualFileSystem@@ vfs, String rootPath) {
        // 1. Récupérer tous les chemins d'images du VFS sous rootPath
        List<String> allPaths = vfs.listContent().stream()
            .map(f -> f.virtualPath())
            .filter(path -> path.startsWith(rootPath))
            .toList();

        int n = allPaths.size();
        if (n == 0) return Stream.empty();

        // 2. Prétraiter toutes les images (grayscale + resize)
        Mat[] images = new Mat[n];
        for (int i = 0; i < n; i++) {
            images[i] = preprocessImage(vfs, allPaths.get(i));
        }

        // 3. Union-Find pour regrouper les images similaires
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        java.util.function.IntUnaryOperator find = new java.util.function.IntUnaryOperator() {
            public int applyAsInt(int x) { 
                return parent[x] == x ? x : (parent[x] = applyAsInt(parent[x])); }
        };
        java.util.function.BiConsumer<Integer, Integer> union = (a, b) -> {
            int ra = find.applyAsInt(a), rb = find.applyAsInt(b);
            if (ra != rb) parent[rb] = ra;
        };

        // 4. Comparaison optimisée (N^2 mais early break, skip si vide)
        for (int i = 0; i < n; i++) {
            if (images[i].empty()) continue;
            for (int j = i + 1; j < n; j++) {
                if (images[j].empty()) continue;
                // Calcul de la similarité par corrélation normalisée
                Mat result = new Mat();
                Imgproc.matchTemplate(images[i], images[j], result, Imgproc.TM_CCOEFF_NORMED);
                double sim = Math.abs(result.get(0,0)[0]);
                result.release();
                if (sim >= SIMILARITY_THRESHOLD) {
                    union.accept(i, j);
                }
            }
        }

        // 5. Regrouper les indices par racine
        java.util.Map<Integer, List<String>> groups = new java.util.HashMap<>();
        for (int i = 0; i < n; i++) {
            if (!images[i].empty()) {
                int root = find.applyAsInt(i);
                groups.computeIfAbsent(root, k -> new java.util.ArrayList<>()).add(allPaths.get(i));
            }
            images[i].release();
        }

        // 6. Retourner uniquement les groupes de doublons (taille > 1)
        return groups.values().stream().filter(list -> list.size() > 1);
    }

    @Override
    public String checkDuplicate(VirtualFileSystem vfs, String incomingPath) {
        return null;
    }

    /**
     * Adaptation VFS : Au lieu de imread(path) qui cherche sur le disque dur,
     * on lit les bytes du VFS et on les décode en mémoire avec imdecode.
     */
    private Mat preprocessImage(VirtualFileSystem vfs, String virtualPath) {
        try {
            // byte[] fileBytes = vfs.readAllBytes(virtualPath); // TODO: A adapter avec ta vraie méthode VFS
            byte[] fileBytes = new byte[0]; // Bouchon

            MatOfByte matOfByte = new MatOfByte(fileBytes);
            Mat src = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
            matOfByte.release();

            if (src.empty()) return src; 

            Mat gray = new Mat();
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
            src.release(); 

            Mat resizedGray = new Mat();
            Imgproc.resize(gray, resizedGray, new Size(256.0, 256.0));
            gray.release(); 

            return resizedGray;
        } catch (Exception e) {
            return new Mat(); // Image corrompue ou introuvable
        }
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 