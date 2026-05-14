package os_p2;

import be.uliege.info0027.deduplication.FileDeduplicationBootstrap;
import be.uliege.info0027.deduplication.FrontendGate;
import be.uliege.info0027.deduplication.StorageChecker;
import be.uliege.info0027.deduplication.VirtualFileSystem;

import os_p2.engine.EngineRouter;
import os_p2.frontend.MyFrontendGate;
import os_p2.storage.MyStorageChecker;

/**
 * C'est le coeur du démarrage de notre application. 
 * C'est ici qu'on branche tous les composants ensemble (Bootstrap).
 */
public class MyDeduplicationBootstrap implements FileDeduplicationBootstrap {

    private VirtualFileSystem vfs;
    private FrontendGate frontendGate;
    private StorageChecker storageChecker;

    /**
     * Méthode appelée par les tests pour tout mettre en route.
     */
    @Override
    public void initialize(VirtualFileSystem vfs) {
        if (vfs == null) {
            throw new IllegalArgumentException("VFS critique manquant. Initialisation avortée.");
        }
        this.vfs = vfs;
        
        EngineRouter engineRouter = new EngineRouter();
        
        this.frontendGate = new MyFrontendGate(this.vfs, engineRouter); 
        
        this.storageChecker = new MyStorageChecker(this.vfs);
        
        System.out.println("[Bootstrap] Architecture os_p2 initialisée avec succès.");
    }

    /**
     * Renvoie la porte d'entrée pour le Frontend.
     */
    @Override
    public FrontendGate getFrontendGate() {
        if (this.frontendGate == null) {
            throw new IllegalStateException("Le système a tenté d'accéder au FrontendGate avant initialize().");
        }
        return this.frontendGate;
    }

    /**
     * Renvoie l'outil de vérification pour le Storage.
     */
    @Override
    public StorageChecker getStorageChecker() {
        if (this.storageChecker == null) {
            throw new IllegalStateException("Le système a tenté d'accéder au StorageChecker avant initialize().");
        }
        return this.storageChecker;
    }
}