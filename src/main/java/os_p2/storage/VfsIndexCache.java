package os_p2.storage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Design Pattern : Repository / In-Memory Cache.
 * Maintient un index thread-safe des fichiers du VFS, regroupés par taille.
 * * Heuristique : La vérification par taille (Size-Based Filtering) permet
 * d'éliminer immédiatement la quasi-totalité des comparaisons de hachage inutiles,
 * garantissant des performances optimales lors de l'upload.
 */
public class VfsIndexCache {

    private final VirtualFileSystem vfs;
    
    // Utilisation d'une Map concurrente pour garantir la sécurité multi-thread
    // Clé = Taille en octets, Valeur = Liste des métadonnées virtuelles
    private Map<Long, List<VirtualFileInfo>> filesBySizeIndex;

    public VfsIndexCache(VirtualFileSystem vfs) {
        this.vfs = vfs;
        this.filesBySizeIndex = new ConcurrentHashMap<>();
        refreshIndex();
    }

    /**
     * Scanne le VFS et reconstruit l'index.
     * Cette méthode doit être appelée au démarrage (Bootstrap) ou 
     * déclenchée périodiquement (Design Pattern : Observer) si le VFS change.
     */
    public final void refreshIndex() {
        // Hypothèse : listContent() renvoie tous les fichiers virtuels
        List<VirtualFileInfo> allFiles = vfs.listContent();
        
        // Grouping thread-safe des fichiers selon leur taille exacte
        this.filesBySizeIndex = allFiles.stream()
            .filter(info -> !info.isDirectory())
            .collect(Collectors.groupingByConcurrent(VirtualFileInfo::size));
            
        System.out.println("[StorageCache] Indexation terminée : " + allFiles.size() + " fichiers virtuels indexés.");
    }

    /**
     * Recherche en O(1) les candidats ayant exactement la même taille.
     * * @param sizeInBytes La taille du fichier physique entrant.
     * @return Une liste immuable des candidats potentiels.
     */
    public List<VirtualFileInfo> getCandidatesBySize(long sizeInBytes) {
        return filesBySizeIndex.getOrDefault(sizeInBytes, Collections.emptyList());
    }
}