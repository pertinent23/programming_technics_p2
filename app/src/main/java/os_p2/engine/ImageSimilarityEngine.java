package os_p2.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.google.common.io.Files;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Design Pattern : Strategy.
 * Moteur spécialisé pour les images via OpenCV (Photography Clients).
 */
public class ImageSimilarityEngine implements DeduplicationEngine {

    private static final double SIMILARITY_THRESHOLD = 0.90; // Exigence du brief 

    static {
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[OpenCV] ERREUR : Librairie native introuvable.");
        }
    }

    @Override
    public Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath) {
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
            @Override
            public int applyAsInt(int x) { 
                return parent[x] == x ? x : (parent[x] = applyAsInt(parent[x])); 
            }
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
        try {
            // 1. Récupérer les métadonnées du fichier entrant
            Optional<VirtualFileInfo> incomingInfo = vfs.listContent().stream()
                .filter(f -> f.virtualPath().equals(incomingPath))
                .findFirst();
            
            if (incomingInfo.isEmpty()) {
                return null; // Le fichier n'existe pas dans le VFS, donc pas de doublon
            }

            // 2. Prétraiter l'image entrante
            Mat incomingImage = preprocessImage(vfs, incomingPath);
            if (incomingImage.empty()) {
                return null; // Impossible de décoder l'image
            }

            // 3. Récupérer tous les autres fichiers du VFS pour la comparaison
            List<String> allPaths = vfs.listContent().stream()
                .map(VirtualFileInfo::virtualPath)
                .filter(path -> !path.equals(incomingPath)) // Exclure le fichier lui-même
                .toList();

            if (allPaths.isEmpty()) {
                incomingImage.release();
                return null; // Aucun autre fichier pour la comparaison
            }

            // 4. Comparer avec tous les autres fichiers
            List<String> duplicates = new ArrayList<>();
            duplicates.add(incomingPath); // Ajouter le fichier lui-même au groupe

            for (String otherPath : allPaths) {
                Mat otherImage = preprocessImage(vfs, otherPath);
                if (otherImage.empty()) {
                    continue; // Impossible de décoder cette image, passer à la suivante
                }

                // Calcul de la similarité par corrélation normalisée
                Mat result = new Mat();
                Imgproc.matchTemplate(incomingImage, otherImage, result, Imgproc.TM_CCOEFF_NORMED);
                double similarity = Math.abs(result.get(0, 0)[0]);
                result.release();
                otherImage.release();

                // Si la similarité est au-dessus du seuil, c'est un doublon
                if (similarity >= SIMILARITY_THRESHOLD) {
                    duplicates.add(otherPath);
                }
            }

            incomingImage.release();

            // 5. Retourner le groupe seulement s'il y a au moins 2 fichiers (doublons)
            if (duplicates.size() > 1) {
                return String.join(",", duplicates);
            }

        } catch (UnsatisfiedLinkError e) {
            System.err.println("[ImageSimilarityEngine] ERREUR : Librairie native OpenCV introuvable.");
        } catch (Exception e) {
            System.err.println("[ImageSimilarityEngine] ERREUR lors de la vérification du doublon : " + e.getMessage());
        }

        return null; // Aucun doublon trouvé
    }

    /**
     * Adaptation VFS : Au lieu de imread(path) qui cherche sur le disque dur,
     * on lit les bytes du VFS et on les décode en mémoire avec imdecode.
     */
    @SuppressWarnings("noused") // Justification : Méthode interne, pas d'API publique à exposer
    private Mat preprocessImage(VirtualFileSystem vfs, String virtualPath) {
        try {
            Optional<VirtualFileInfo> fileInfo = vfs.listContent().stream()
                .filter(f -> f.virtualPath().equals(virtualPath))
                .findFirst();
            
                
            if (fileInfo.isEmpty()) 
                throw new IllegalArgumentException("Fichier non trouvé : " + virtualPath);

            byte[] fileBytes = Files.toByteArray(
                new File(fileInfo.get().physicalPath())
            );

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
        } catch (IllegalArgumentException e) {
            return new Mat(); // Image corrompue ou introuvable
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[ImageSimilarityEngine] ERREUR : Librairie native introuvable.");
            return new Mat();
        } catch (IOException e) {
            System.err.println("[ImageSimilarityEngine] ERREUR lors du prétraitement de l'image : " + e.getMessage());
            return new Mat();
        }
    }
}
