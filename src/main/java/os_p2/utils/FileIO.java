package os_p2.utils;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Cette classe gère la lecture des fichiers physiques sur le disque.
 * On utilise des buffers pour ne pas saturer la mémoire (évite les SIGKILL).
 */
public class FileIO {

    /**
     * Compare deux fichiers octet par octet pour être sûr qu'ils sont identiques.
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
     * Calcule l'empreinte numérique (SHA-256) d'un fichier.
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
