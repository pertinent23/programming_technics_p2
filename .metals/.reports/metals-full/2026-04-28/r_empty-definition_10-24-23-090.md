error id: file://<WORKSPACE>/app/src/main/java/os_p2/engine/PureJavaHashEngine.java:_empty_/VirtualFileSystem#listContent#
file://<WORKSPACE>/app/src/main/java/os_p2/engine/PureJavaHashEngine.java
empty definition using pc, found symbol in pc: _empty_/VirtualFileSystem#listContent#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1918
uri: file://<WORKSPACE>/app/src/main/java/os_p2/engine/PureJavaHashEngine.java
text:
```scala
package os_p2.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.google.common.hash.Hashing;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Design Pattern : Strategy (Concrete Strategy).
 * Implémentation 100% Java (Seamless Swap Fallback)[cite: 14].
 */
public class PureJavaHashEngine implements DeduplicationEngine {

    @Override
    public Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath) {
        // Map pour regrouper les chemins par leur Hash SHA-256
        Map<String, List<String>> hashToPaths = new HashMap<>();

        // Récupérer tous les fichiers du VFS
        List<String> allFiles = vfs.listContent().stream()
            .map(VirtualFileInfo::virtualPath)
            .filter(path -> path.startsWith(rootPath))
            .map(path -> path.substring(rootPath.length()))
            .toList();

        for (String path : allFiles) {
            try {
                // On récupère le contenu via le VFS
                // byte[] content = vfs.readAllBytes(path);
                byte[] content = new byte[0]; // Bouchon
                
                String hash = Hashing.sha256().hashBytes(content).toString();
                hashToPaths.computeIfAbsent(hash, k -> new ArrayList<>()).add(path);
            } catch (Exception e) {
                System.err.println("Erreur de lecture VFS pour " + path);
            }
        }

        // On ne conserve que les groupes ayant au moins 2 fichiers (des doublons)
        return hashToPaths.values().stream().filter(list -> list.size() > 1);
    }

    @Override
    public String checkDuplicate(VirtualFileSystem vfs, String incomingPath) {
        // 1. Récupérer tous les fichiers du VFS
        List<VirtualFileInfo> allFiles = vfs.listCon@@tent();

        // 2. Regrouper par taille (premier filtre)
        Map<Long, List<VirtualFileInfo>> sizeGroups = new HashMap<>();
        for (VirtualFileInfo file : allFiles) {
            sizeGroups.computeIfAbsent(file.size(), k -> new ArrayList<>()).add(file);
        }

        // 3. Trouver le groupe de même taille que incomingPath
        VirtualFileInfo incoming = allFiles.stream()
            .filter(f -> f.virtualPath().equals(incomingPath))
            .findFirst().orElse(null);
        if (incoming == null) return null;
        List<VirtualFileInfo> candidates = sizeGroups.getOrDefault(incoming.size(), List.of());
        if (candidates.size() <= 1) return null;

        // 4. Calculer le hash (SHA-256) pour chaque fichier du groupe
        Map<String, List<VirtualFileInfo>> hashGroups = new HashMap<>();
        String incomingHash = null;
        for (VirtualFileInfo file : candidates) {
            try {
                // byte[] content = vfs.readAllBytes(file.virtualPath());
                byte[] content = new byte[0]; // Bouchon à remplacer par lecture réelle
                String hash = Hashing.sha256().hashBytes(content).toString();
                hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
                if (file.virtualPath().equals(incomingPath)) {
                    incomingHash = hash;
                }
            } catch (Exception e) {
                // Ignore les erreurs de lecture
            }
        }
        if (incomingHash == null) return null;

        // 5. Vérifier les vrais doublons (byte à byte)
        List<VirtualFileInfo> hashCandidates = hashGroups.getOrDefault(incomingHash, List.of());
        if (hashCandidates.size() <= 1) return null;

        List<String> duplicates = new ArrayList<>();
        byte[] incomingContent = new byte[0]; // vfs.readAllBytes(incomingPath); // Bouchon
        for (VirtualFileInfo file : hashCandidates) {
            try {
                // byte[] content = vfs.readAllBytes(file.virtualPath());
                byte[] content = new byte[0]; // Bouchon
                if (file.virtualPath().equals(incomingPath) || java.util.Arrays.equals(content, incomingContent)) {
                    duplicates.add(file.virtualPath());
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        // On ne retourne que s'il y a au moins 2 fichiers dans le groupe
        if (duplicates.size() > 1) {
            return String.join(",", duplicates);
        }
        return null;
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/VirtualFileSystem#listContent#