package os_p2.engine;

import java.util.List;
import java.util.stream.Stream;

import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Design Pattern : Strategy.
 * Contrat unifié pour tous les moteurs de déduplication.
 * Obligation stricte : N'utiliser QUE le VirtualFileSystem, jamais java.io.File.
 */
public interface DeduplicationEngine {

    /**
     * Besoin de l'équipe Frontend : Scanner un dossier entier.
     * * @param vfs Le système de fichiers virtuel.
     * @param rootPath Le chemin virtuel racine à scanner.
     * @return Un flux (Stream) paresseux de groupes de doublons.
     */
    Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath);

    /**
     * Anticipation pour l'équipe Storage : Vérifier un fichier à la volée lors de l'upload.
     * * @param vfs Le système de fichiers virtuel.
     * @param incomingPath Le chemin du fichier entrant.
     * @return Le chemin du fichier original existant, ou null si c'est un nouveau fichier.
     */
    String checkDuplicate(VirtualFileSystem vfs, String incomingPath);
}