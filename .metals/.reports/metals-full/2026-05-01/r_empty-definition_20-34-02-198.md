error id: file://<WORKSPACE>/app/src/main/java/os_p2/storage/MyStorageChecker.java:java/io/IOException#
file://<WORKSPACE>/app/src/main/java/os_p2/storage/MyStorageChecker.java
empty definition using pc, found symbol in pc: java/io/IOException#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 39
uri: file://<WORKSPACE>/app/src/main/java/os_p2/storage/MyStorageChecker.java
text:
```scala
package os_p2.storage;

import java.io.@@IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import be.uliege.info0027.deduplication.StorageChecker;
import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;
import os_p2.engine.DeduplicationEngine;

/**
 * Design Pattern : Interceptor / Facade.
 * S'intègre dans le pipeline d'upload de l'équipe Storage.
 * Son rôle est d'empêcher l'upload physique si le fichier existe déjà dans le VFS.
 */
public class MyStorageChecker implements StorageChecker {

    private final VfsIndexCache indexCache;
    private final DeduplicationEngine engine;
    private final VirtualFileSystem vfs;

    // Injection de dépendances (Dependency Injection)
    public MyStorageChecker(VirtualFileSystem vfs, VfsIndexCache indexCache, DeduplicationEngine engine) {
        this.vfs = vfs;
        this.indexCache = indexCache;
        this.engine = engine;
    }

    /**
     * Intercepte le fichier à la volée.
     * * @param incomingFilePath Le chemin physique (NIO) du fichier en cours d'upload.
     * @return Les métadonnées du fichier virtuel existant, ou null s'il est unique.
     */
    @Override
    public VirtualFileInfo findDuplicate(Path incomingFilePath) {
        try {
            // 1. Validation NIO stricte
            if (incomingFilePath == null || !Files.isRegularFile(incomingFilePath)) {
                return null;
            }
            
            // 2. Extraction de la taille sans charger le fichier en RAM
            long incomingSize = Files.size(incomingFilePath);

            // 3. Filtrage O(1) via le cache
            List<VirtualFileInfo> candidatesInfo = indexCache.getCandidatesBySize(incomingSize);
            
            // Si aucune taille ne correspond, c'est obligatoirement un nouveau fichier
            if (candidatesInfo.isEmpty()) {
                return null; 
            }

            // 4. Vérification profonde (Deep Check)
            // On convertit le chemin physique en String pour l'interface de notre Moteur.
            // Le Moteur se chargera de comparer ce fichier physique avec les candidats du VFS.
            String duplicateVirtualPath = engine.checkDuplicate(this.vfs, incomingFilePath.toString());

            // 5. Récupération de l'objet VirtualFileInfo correspondant au doublon trouvé
            if (duplicateVirtualPath != null) {
                return candidatesInfo.stream()
                    .filter(info -> duplicateVirtualPath.equals(info.virtualPath()))
                    .findFirst()
                    .orElse(null);
            }

        } catch (IOException e) {
            System.err.println("[StorageChecker] Erreur d'accès I/O sur le fichier entrant : " + e.getMessage());
        }

        // Aucun doublon exact n'a été trouvé par le moteur
        return null; 
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: java/io/IOException#