package os_p2.storage;

import java.nio.file.Path;
import be.uliege.info0027.deduplication.StorageChecker;
import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Cette classe sert à l'équipe Storage pour vérifier les doublons lors des uploads.
 * On veut éviter de stocker deux fois le même fichier sur le disque dur.
 */
public class MyStorageChecker implements StorageChecker {

    private final VirtualFileSystem vfs;

    public MyStorageChecker(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    /**
     * Cette méthode intercepte le fichier avant qu'il soit stocké définitivement.
     * Si elle trouve un doublon, l'upload peut être annulé.
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