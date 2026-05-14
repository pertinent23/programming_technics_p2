package os_p2.storage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Ce cache permet de stocker les fichiers du VFS triés par taille.
 * Ça nous évite de comparer des fichiers qui n'ont pas la même taille !
 */
public class VfsIndexCache {

    private final VirtualFileSystem vfs;
    
    private Map<Long, List<VirtualFileInfo>> filesBySizeIndex;

    public VfsIndexCache(VirtualFileSystem vfs) {
        this.vfs = vfs;
        this.filesBySizeIndex = new ConcurrentHashMap<>();
        refreshIndex();
    }

    /**
     * On reconstruit tout l'index en scannant le VFS.
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
     * Renvoie tous les fichiers qui font précisément cette taille-là.
     */
    public List<VirtualFileInfo> getCandidatesBySize(long size) {
        return filesBySizeIndex.getOrDefault(size, Collections.emptyList());
    }

    /**
     * Ajoute un nouveau fichier dans notre index (utile après un upload réussi).
     */
    public void addToIndex(VirtualFileInfo info) {
        if (info != null) {
            filesBySizeIndex.computeIfAbsent(info.size(), s -> new java.util.ArrayList<>()).add(info);
        }
    }
}