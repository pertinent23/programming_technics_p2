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
 * Ce moteur est spécial : il ne cherche pas les fichiers identiques au bit près,
 * mais les images qui se ressemblent visuellement grâce à OpenCV.
 */
public class ImageSimilarityEngine implements DeduplicationEngine {

    private static final double SIMILARITY_THRESHOLD = 0.90;

    static {
        try {
            nu.pattern.OpenCV.loadShared();
        } catch (Exception e) {
            System.err.println("[OpenCV] ERREUR : Impossible de charger la librairie native OpenCV.");
        }
    }

    /**
     * Scanne les images d'un utilisateur et utilise "Union-Find" pour regrouper 
     * celles qui ont une similarité de plus de 90%.
     */
    @Override
    public Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath, String user) {
        List<VirtualFileInfo> allFiles = os_p2.utils.VfsScanner.scanForUser(vfs, rootPath, user);

        int n = allFiles.size();
        if (n == 0) {
            return Stream.empty();
        }
        
        Mat[] images = new Mat[n];
        for (int i = 0; i < n; i++) {
            images[i] = preprocessPhysicalImage(allFiles.get(i).physicalPath());
        }

        int[] parent = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }

        java.util.function.IntUnaryOperator find = new java.util.function.IntUnaryOperator() {
            @Override
            public int applyAsInt(int x) {
                return parent[x] == x ? x : (parent[x] = applyAsInt(parent[x]));
            }
        };

        java.util.function.BiConsumer<Integer, Integer> union = (a, b) -> {
            int ra = find.applyAsInt(a), rb = find.applyAsInt(b);
            if (ra != rb) {
                parent[rb] = ra;
            }
        };

        for (int i = 0; i < n; i++) {
            if (images[i].empty()) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                if (images[j].empty()) {
                    continue;
                }
                Mat res = new Mat();
                Imgproc.matchTemplate(images[i], images[j], res, Imgproc.TM_CCOEFF_NORMED);
                double sim = Math.abs(res.get(0, 0)[0]);
                res.release();
                if (sim >= SIMILARITY_THRESHOLD) {
                    union.accept(i, j);
                }
            }
        }

        Map<Integer, List<String>> groups = new HashMap<>();
        for (int i = 0; i < n; i++) {
            if (!images[i].empty()) {
                int root = find.applyAsInt(i);
                groups.computeIfAbsent(root, r -> new ArrayList<>()).add(allFiles.get(i).virtualPath());
            }
            images[i].release();
        }

        return groups.values().stream().filter(list -> list.size() > 1);
    }

    /**
     * Vérifie si une nouvelle image ressemble à une image déjà présente sur le serveur.
     */
    @Override
    public String checkDuplicate(VirtualFileSystem vfs, String incomingPath) {
        Mat incomingImage = preprocessPhysicalImage(incomingPath);
        if (incomingImage.empty()) {
            return null;
        }

        for (VirtualFileInfo info : os_p2.utils.VfsScanner.scanEverything(vfs)) {
            String physPath = info.physicalPath();
            if (physPath == null) {
                continue;
            }

            Mat otherImage = preprocessPhysicalImage(physPath);
            if (otherImage.empty()) {
                continue;
            }

            Mat res = new Mat();
            Imgproc.matchTemplate(incomingImage, otherImage, res, Imgproc.TM_CCOEFF_NORMED);
            double sim = Math.abs(res.get(0, 0)[0]);
            res.release();
            otherImage.release();

            if (sim >= SIMILARITY_THRESHOLD) {
                incomingImage.release();
                return info.virtualPath();
            }
        }
        incomingImage.release();
        return null;
    }

    /**
     * Prépare une image : elle est lue, passée en gris et redimensionnée en 256x256.
     * C'est nécessaire pour pouvoir comparer les images entre elles.
     */
    private Mat preprocessPhysicalImage(String physicalPath) {
        try {
            if (physicalPath == null) {
                return new Mat();
            }

            byte[] fileBytes = Files.readAllBytes(Path.of(physicalPath));
            MatOfByte matOfByte = new MatOfByte(fileBytes);
            Mat src = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR);
            matOfByte.release();

            if (src.empty()) {
                return src;
            }

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
