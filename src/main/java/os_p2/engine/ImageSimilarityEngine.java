package os_p2.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Design Pattern : Strategy.
 * Moteur spécialisé pour la déduplication par similarité d'images via OpenCV.
 * Destiné aux Photography Clients (cf. brief section 2.3).
 */
public class ImageSimilarityEngine implements DeduplicationEngine {

    /** Seuil de similarité — 0.90 exigé par le brief */
    private static final double SIMILARITY_THRESHOLD = 0.90;

    static {
        try {
            nu.pattern.OpenCV.loadShared();
        } catch (Exception e) {
            System.err.println("[OpenCV] ERREUR : Impossible de charger la librairie native OpenCV.");
        }
    }

    /**
     * Scanne les images pour un utilisateur et regroupe celles similaires via Union-Find.
     */
    @Override
    public Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath, String user) {
        // Réutilise la méthode partagée de PureJavaHashEngine
        List<VirtualFileInfo> allFiles = PureJavaHashEngine.getFilesForUser(vfs, rootPath, user);

        int n = allFiles.size();
        if (n == 0) return Stream.empty();

        // Prétraiter toutes les images (grayscale + resize 256×256)
        Mat[] images = new Mat[n];
        for (int i = 0; i < n; i++) {
            images[i] = preprocessPhysicalImage(allFiles.get(i).physicalPath());
        }

        // Union-Find pour regrouper les images similaires
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        java.util.function.IntUnaryOperator find = new java.util.function.IntUnaryOperator() {
            @Override
            public int applyAsInt(int x) {
                return parent[x] == x ? x : (parent[x] = applyAsInt(parent[x]));
            }
        };

        java.util.function.BiConsumer<Integer, Integer> union = (a, b) -> {
            int ra = find.applyAsInt(a), rb = find.applyAsInt(b);
            if (ra != rb) parent[rb] = ra;
        };

        // Comparaison N×N (skip si image vide/corrompue)
        for (int i = 0; i < n; i++) {
            if (images[i].empty()) continue;
            for (int j = i + 1; j < n; j++) {
                if (images[j].empty()) continue;
                Mat result = new Mat();
                Imgproc.matchTemplate(images[i], images[j], result, Imgproc.TM_CCOEFF_NORMED);
                double sim = Math.abs(result.get(0, 0)[0]);
                result.release();
                if (sim >= SIMILARITY_THRESHOLD) {
                    union.accept(i, j);
                }
            }
        }

        // Regrouper les chemins virtuels par racine Union-Find
        Map<Integer, List<String>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (!images[i].empty()) {
                int root = find.applyAsInt(i);
                groups.computeIfAbsent(root, _ -> new ArrayList<>()).add(allFiles.get(i).virtualPath());
            }
            images[i].release();
        }

        return groups.values().stream().filter(list -> list.size() > 1);
    }

    /**
     * Vérifie si un fichier physique entrant est similaire à un fichier du VFS.
     */
    @Override
    public String checkDuplicate(VirtualFileSystem vfs, String incomingPath) {
        try {
            if (!Files.isRegularFile(Path.of(incomingPath))) return null;

            Mat incomingImage = preprocessPhysicalImage(incomingPath);
            if (incomingImage.empty()) return null;

            for (VirtualFileInfo info : vfs.listContent()) {
                if (Boolean.TRUE.equals(info.isDirectory())) continue;
                if (info.physicalPath() == null) continue;

                Mat otherImage = preprocessPhysicalImage(info.physicalPath());
                if (otherImage.empty()) continue;

                Mat result = new Mat();
                Imgproc.matchTemplate(incomingImage, otherImage, result, Imgproc.TM_CCOEFF_NORMED);
                double similarity = Math.abs(result.get(0, 0)[0]);
                result.release();
                otherImage.release();

                if (similarity >= SIMILARITY_THRESHOLD) {
                    incomingImage.release();
                    return info.virtualPath();
                }
            }
            incomingImage.release();

        } catch (Exception e) {
            System.err.println("[ImageSimilarityEngine] ERREUR : " + e.getMessage());
        }
        return null;
    }

    // ─── Utilitaires ───────────────────────────────────────────────────────

    /**
     * Lit les bytes depuis le chemin physique et les décode en grayscale 256×256.
     */
    private Mat preprocessPhysicalImage(String physicalPath) {
        try {
            if (physicalPath == null) return new Mat();

            byte[] fileBytes = Files.readAllBytes(Path.of(physicalPath));
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
            return new Mat();
        }
    }
}
