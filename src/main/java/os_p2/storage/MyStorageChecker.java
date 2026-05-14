package os_p2.storage;

import java.nio.file.Path;
import be.uliege.info0027.deduplication.StorageChecker;
import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Implémentation du service de vérification de stockage cross-user.
 * Intervient lors des uploads pour détecter si le contenu d'un fichier est déjà présent.
 */
public class MyStorageChecker implements StorageChecker {

    private final VirtualFileSystem vfs;

    /**
     * Construit le vérificateur avec le VFS spécifié.
     * 
     * @param vfs Le système de fichiers virtuel à utiliser.
     */
    public MyStorageChecker(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    /**
     * Recherche un doublon pour un fichier entrant avant son stockage définitif.
     * 
     * @param incomingFilePath Le chemin vers le fichier temporaire qui vient d'être uploadé.
     * @return Les métadonnées du doublon original trouvé, ou null.
     */
    @Override
    public VirtualFileInfo findDuplicate(Path incomingFilePath) {
        for (VirtualFileInfo file : os_p2.utils.VfsScanner.scanEverything(vfs)) {
            String physPath = file.physicalPath();
            if (physPath == null) {
                continue;
            }

            Path targetPath = Path.of(physPath);
            
            try {
                if (java.nio.file.Files.isSameFile(incomingFilePath, targetPath)) {
                    continue;
                }
            } catch (Exception ignored) {
                if (incomingFilePath.toString().equals(physPath)) {
                    continue;
                }
            }

            if (os_p2.utils.FileIO.areIdentical(incomingFilePath, targetPath)) {
                return file;
            }
        }
        return null;
    }
}