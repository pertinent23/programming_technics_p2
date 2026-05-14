package os_p2.utils;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Service utilitaire pour les opérations d'Entrée/Sortie sur les fichiers physiques.
 * Optimisé pour une consommation mémoire minimale ($O(1)$) afin d'éviter les dépassements de RAM.
 */
public class FileIO {

    /**
     * Compare le contenu binaire de deux fichiers physiques.
     * 
     * @param p1 Chemin du premier fichier.
     * @param p2 Chemin du second fichier.
     * @return true si les fichiers ont exactement le même contenu.
     */
    public static boolean areIdentical(Path p1, Path p2) {
        try {
            if (Files.size(p1) != Files.size(p2)) {
                return false;
            }

            try (var is1 = new BufferedInputStream(Files.newInputStream(p1));
                 var is2 = new BufferedInputStream(Files.newInputStream(p2))) {
                int b1, b2;
                while ((b1 = is1.read()) != -1) {
                    b2 = is2.read();
                    if (b1 != b2) {
                        return false;
                    }
                }
                return is2.read() == -1;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calcule l'empreinte SHA-256 d'un fichier physique via un flux bufferisé.
     * 
     * @param path Le chemin du fichier.
     * @return L'empreinte hexadécimale (String), ou null en cas d'erreur.
     */
    public static String calculateSha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var is = new BufferedInputStream(Files.newInputStream(path))) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, n);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            return null;
        }
    }
}
