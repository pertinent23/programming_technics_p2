package os_p2.storage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Cache mémoire pour indexer les fichiers du VFS par leur taille.
 * Permet d'optimiser les recherches de doublons en filtrant les candidats par taille.
 */
public class VfsIndexCache {

    private final VirtualFileSystem vfs;
    
    private Map<Long, List<VirtualFileInfo>> filesBySizeIndex;

    /**
     * Initialise le cache pour un VFS donné.
     * 
     * @param vfs Le système de fichiers virtuel à indexer.
     */
    public VfsIndexCache(VirtualFileSystem vfs) {
        this.vfs = vfs;
        this.filesBySizeIndex = new ConcurrentHashMap<>();
        refreshIndex();
    }

    /**
     * Reconstruit intégralement l'index en parcourant tout le VFS.
     */
    public synchronized void refreshIndex() {
        try {
            List<VirtualFileInfo> allFiles = os_p2.utils.VfsScanner.scanEverything(vfs);
            
            this.filesBySizeIndex = allFiles.stream()
                .filter(f -> f != null)
                .collect(Collectors.groupingBy(
                    VirtualFileInfo::size,
                    ConcurrentHashMap::new,
                    Collectors.toList()
                ));
        } catch (Exception ignored) {
        }
    }

    /**
     * Récupère la liste des fichiers ayant une taille spécifique.
     * 
     * @param size La taille recherchée en octets.
     * @return Une liste de fichiers candidats.
     */
    public List<VirtualFileInfo> getCandidatesBySize(long size) {
        return filesBySizeIndex.getOrDefault(size, Collections.emptyList());
    }

    /**
     * Ajoute un fichier à l'index sans reconstruire l'intégralité du cache.
     * 
     * @param info Les métadonnées du fichier à ajouter.
     */
    public void addToIndex(VirtualFileInfo info) {
        if (info != null) {
            filesBySizeIndex.computeIfAbsent(info.size(), s -> new java.util.ArrayList<>()).add(info);
        }
    }
}