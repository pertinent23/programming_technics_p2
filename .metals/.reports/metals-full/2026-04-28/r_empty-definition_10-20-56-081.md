error id: file://<WORKSPACE>/app/src/main/java/os_p2/engine/PureJavaHashEngine.java:VirtualFileSystem#
file://<WORKSPACE>/app/src/main/java/os_p2/engine/PureJavaHashEngine.java
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 568
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
    public Stream<List<String>> scan(VirtualFileSystem@@ vfs, String rootPath) {
        // Map pour regrouper les chemins par leur Hash SHA-256
        Map<String, List<String>> hashToPaths = new HashMap<>();

        // Récupérer tous les fichiers du VFS
        List<String> all = vfs.listContent().stream()
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
        // Implémentation future pour l'équipe Storage
        return null;
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: 