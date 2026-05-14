package os_p2;

import be.uliege.info0027.deduplication.FileDeduplicationBootstrap;
import be.uliege.info0027.deduplication.FrontendGate;
import be.uliege.info0027.deduplication.StorageChecker;
import be.uliege.info0027.deduplication.VirtualFileSystem;

import os_p2.engine.EngineRouter;
import os_p2.frontend.MyFrontendGate;
import os_p2.storage.MyStorageChecker;

/**
 * Point d'entrée de configuration et d'initialisation du système de déduplication.
 * Responsable de l'instanciation et du câblage de tous les composants majeurs.
 */
public class MyDeduplicationBootstrap implements FileDeduplicationBootstrap {

    private VirtualFileSystem vfs;
    private FrontendGate frontendGate;
    private StorageChecker storageChecker;

    /**
     * Initialise l'architecture complète avec le VFS fourni.
     * 
     * @param vfs Le système de fichiers virtuel utilisé comme source de données.
     * @throws IllegalArgumentException Si le VFS fourni est null.
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
     * Récupère l'implémentation de la passerelle Frontend.
     * 
     * @return L'instance de FrontendGate configurée.
     * @throws IllegalStateException Si la méthode est appelée avant initialize().
     */
    @Override
    public FrontendGate getFrontendGate() {
        if (this.frontendGate == null) {
            throw new IllegalStateException("Le système a tenté d'accéder au FrontendGate avant initialize().");
        }
        return this.frontendGate;
    }

    /**
     * Récupère l'implémentation du vérificateur de stockage.
     * 
     * @return L'instance de StorageChecker configurée.
     * @throws IllegalStateException Si la méthode est appelée avant initialize().
     */
    @Override
    public StorageChecker getStorageChecker() {
        if (this.storageChecker == null) {
            throw new IllegalStateException("Le système a tenté d'accéder au StorageChecker avant initialize().");
        }
        return this.storageChecker;
    }
}