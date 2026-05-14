package os_p2.engine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * C'est notre moteur de déduplication fait maison en Java.
 * Il calcule les hashs SHA-256 pour trouver les fichiers identiques.
 */
public class PureJavaHashEngine implements DeduplicationEngine {

    /**
     * Cette méthode scanne tout le VFS pour un utilisateur et regroupe 
     * les fichiers qui ont exactement le même contenu (même hash).
     */
    @Override
    public Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath, String user) {
        Map<String, List<String>> hashToPaths = new HashMap<>();

        List<VirtualFileInfo> files = os_p2.utils.VfsScanner.scanForUser(vfs, rootPath, user);

        for (VirtualFileInfo file : files) {
            String physPath = file.physicalPath();
            if (physPath == null) {
                continue;
            }
            
            String hash = os_p2.utils.FileIO.calculateSha256(Path.of(physPath));
            if (hash != null) {
                hashToPaths.computeIfAbsent(hash, h -> new ArrayList<>()).add(file.virtualPath());
            }
        }

        return hashToPaths.values().stream().filter(list -> list.size() > 1);
    }

    /**
     * Ici on vérifie si un fichier qui arrive (pendant un upload par exemple)
     * existe déjà quelque part dans le système de fichiers.
     */
    @Override
    public String checkDuplicate(VirtualFileSystem vfs, String incomingPath) {
        Path incomingNioPath = Path.of(incomingPath);
        
        for (VirtualFileInfo file : os_p2.utils.VfsScanner.scanEverything(vfs)) {
            String physPath = file.physicalPath();
            if (physPath == null || incomingPath.equals(physPath)) {
                continue;
            }

            if (os_p2.utils.FileIO.areIdentical(incomingNioPath, Path.of(physPath))) {
                return file.virtualPath();
            }
        }
        return null;
    }
}