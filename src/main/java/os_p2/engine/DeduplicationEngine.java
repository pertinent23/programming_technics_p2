package os_p2.engine;

import java.util.List;
import java.util.stream.Stream;

import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Interface commune pour tous les moteurs de déduplication.
 * Elle définit les deux actions principales : scanner un dossier ou vérifier un fichier à la volée.
 */
public interface DeduplicationEngine {

    /**
     * Scanne un dossier racine pour un utilisateur donné afin de trouver des groupes de doublons.
     * 
     * @param vfs Le système de fichiers virtuel à explorer.
     * @param rootPath Le chemin virtuel racine du scan.
     * @param user L'identifiant de l'utilisateur propriétaire des fichiers.
     * @return Un flux de listes, où chaque liste contient les chemins virtuels de fichiers identiques ou similaires.
     */
    Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath, String user);

    /**
     * Vérifie si un fichier entrant existe déjà dans le système de fichiers global.
     * 
     * @param vfs Le système de fichiers virtuel utilisé pour la comparaison.
     * @param incomingPath Le chemin physique du fichier à tester.
     * @return Le chemin virtuel du premier doublon trouvé, ou null si aucune correspondance n'est détectée.
     */
    String checkDuplicate(VirtualFileSystem vfs, String incomingPath);
}