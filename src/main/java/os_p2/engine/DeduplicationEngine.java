package os_p2.engine;

import java.util.List;
import java.util.stream.Stream;

import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * C'est l'interface commune pour tous nos moteurs de déduplication.
 * Elle définit les deux actions principales : scanner un dossier ou vérifier un fichier.
 */
public interface DeduplicationEngine {

    /**
     * Pour scanner tout un dossier et trouver les groupes de doublons.
     */
    Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath, String user);

    /**
     * Pour voir si un fichier qu'on vient de recevoir existe déjà sur le serveur.
     */
    String checkDuplicate(VirtualFileSystem vfs, String incomingPath);
}