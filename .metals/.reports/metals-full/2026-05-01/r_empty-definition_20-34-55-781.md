error id: file://<WORKSPACE>/app/src/main/java/os_p2/MyDeduplicationBootstrap.java:os_p2/frontend/MyFrontendGate#
file://<WORKSPACE>/app/src/main/java/os_p2/MyDeduplicationBootstrap.java
empty definition using pc, found symbol in pc: os_p2/frontend/MyFrontendGate#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 310
uri: file://<WORKSPACE>/app/src/main/java/os_p2/MyDeduplicationBootstrap.java
text:
```scala
package os_p2;

import be.uliege.info0027.deduplication.FileDeduplicationBootstrap;
import be.uliege.info0027.deduplication.FrontendGate;
import be.uliege.info0027.deduplication.StorageChecker;
import be.uliege.info0027.deduplication.VirtualFileSystem;

import os_p2.engine.EngineRouter;
import os_p2.frontend.@@MyFrontendGate;
import os_p2.storage.MyStorageChecker;
import os_p2.storage.VfsIndexCache;

/**
 * Design Pattern : Composition Root / Abstract Factory.
 * Point d'entrée principal exigé par le système de test automatisé.
 * Responsabilité : Instancier, configurer et lier tous les composants 
 * de l'architecture (Injection de Dépendances manuelle).
 */
public class MyDeduplicationBootstrap implements FileDeduplicationBootstrap {

    private VirtualFileSystem vfs;
    private FrontendGate frontendGate;
    private StorageChecker storageChecker;

    /**
     * Méthode appelée en premier par le système de test de l'université.
     */
    @Override
    public void initialize(VirtualFileSystem vfs) {
        if (vfs == null) {
            throw new IllegalArgumentException("VFS critique manquant. Initialisation avortée.");
        }
        this.vfs = vfs;
        
        // 1. Initialisation des services partagés (Core)
        VfsIndexCache indexCache = new VfsIndexCache(this.vfs);
        EngineRouter engineRouter = new EngineRouter();
        
        // 2. Injection des dépendances dans le Frontend (a besoin du VFS et du routeur)
        // Note : Assure-toi d'ajouter le constructeur MyFrontendGate(VirtualFileSystem, EngineRouter)
        this.frontendGate = new MyFrontendGate(this.vfs, engineRouter); 
        
        // 3. Injection des dépendances dans le Storage (a besoin du VFS, du Cache, et du Moteur Exact)
        this.storageChecker = new MyStorageChecker(this.vfs, indexCache, engineRouter.getEngine("exact"));
        
        System.out.println("[Bootstrap] Architecture os_p2 initialisée avec succès.");
    }

    @Override
    public FrontendGate getFrontendGate() {
        if (this.frontendGate == null) {
            throw new IllegalStateException("Le système a tenté d'accéder au FrontendGate avant initialize().");
        }
        return this.frontendGate;
    }

    @Override
    public StorageChecker getStorageChecker() {
        if (this.storageChecker == null) {
            throw new IllegalStateException("Le système a tenté d'accéder au StorageChecker avant initialize().");
        }
        return this.storageChecker;
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: os_p2/frontend/MyFrontendGate#