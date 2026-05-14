error id: file://<WORKSPACE>/src/main/java/os_p2/engine/DeduplicationEngine.java:_empty_/VirtualFileSystem#
file://<WORKSPACE>/src/main/java/os_p2/engine/DeduplicationEngine.java
empty definition using pc, found symbol in pc: _empty_/VirtualFileSystem#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 708
uri: file://<WORKSPACE>/src/main/java/os_p2/engine/DeduplicationEngine.java
text:
```scala
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
     * Besoin de l'équipe Frontend : Scanner un dossier entier pour un utilisateur.
     * @param vfs Le système de fichiers virtuel.
     * @param rootPath Le chemin virtuel racine à scanner.
     * @param user L'utilisateur cible.
     * @return Un flux (Stream) paresseux de groupes de doublons.
     */
    Stream<List<String>> scan(Virtua@@lFileSystem vfs, String rootPath, String user);

    /**
     * Anticipation pour l'équipe Storage : Vérifier un fichier à la volée lors de l'upload.
     * @param vfs Le système de fichiers virtuel.
     * @param incomingPath Le chemin physique du fichier entrant.
     * @return Le chemin virtuel du fichier original existant, ou null si c'est un nouveau fichier.
     */
    String checkDuplicate(VirtualFileSystem vfs, String incomingPath);
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/VirtualFileSystem#