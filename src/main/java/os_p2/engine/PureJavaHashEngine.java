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
 * Moteur de déduplication implémenté en Java pur utilisant l'algorithme SHA-256.
 * Il permet de détecter les doublons exacts par comparaison d'empreintes numériques.
 */
public class PureJavaHashEngine implements DeduplicationEngine {

    /**
     * Scanne le VFS pour un utilisateur et regroupe les fichiers ayant le même hash SHA-256.
     * 
     * @param vfs Le système de fichiers virtuel.
     * @param rootPath Le chemin virtuel racine à explorer.
     * @param user L'utilisateur dont on scanne les fichiers.
     * @return Un flux de groupes de chemins virtuels doublons.
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
     * Vérifie si un fichier entrant est identique à un fichier déjà présent sur le disque.
     * 
     * @param vfs Le système de fichiers virtuel global.
     * @param incomingPath Le chemin physique du fichier entrant.
     * @return Le chemin virtuel d'un doublon existant, ou null sinon.
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