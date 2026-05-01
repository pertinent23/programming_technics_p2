error id: file://<WORKSPACE>/app/src/main/java/os_p2/engine/LegacyNativeEngine.java:java/util/stream/Stream#
file://<WORKSPACE>/app/src/main/java/os_p2/engine/LegacyNativeEngine.java
empty definition using pc, found symbol in pc: java/util/stream/Stream#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 1948
uri: file://<WORKSPACE>/app/src/main/java/os_p2/engine/LegacyNativeEngine.java
text:
```scala
package os_p2.engine;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment; // Ton binding jextract
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import be.uliege.info0027.deduplication.VirtualFileSystem;
import os_p2.native_c.filededup_h;

/**
 * Design Pattern : Adapter & Strategy.
 * Pont FFM API vers la librairie C optimisée.
 */
public class LegacyNativeEngine implements DeduplicationEngine {

    static {
        // Cette ligne lèvera une UnsatisfiedLinkError si .so/.dll est absent,
        // ce qui sera intercepté par notre EngineRouter pour le Seamless Swap.
        System.loadLibrary("filededup"); 
    }

    @Override
    public Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath) {
        List<List<String>> allDuplicateGroups = new ArrayList<>();

        // Arena.ofConfined() gère la mémoire native automatiquement (try-with-resources)
        try (Arena arena = Arena.ofConfined()) {
            
            MemorySegment fdState = filededup_h.FDInit();
            if (fdState.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Erreur critique : FDInit a retourné NULL.");
            }

            // Récupérer tous les fichiers du VFS
            List<String> paths = vfs.listAllFiles(rootPath);

            // 1. On injecte tous les fichiers dans le moteur C
            for (String path : paths) {
                MemorySegment cString = arena.allocateFrom(path);
                filededup_h.FDCheck(fdState, cString);
            }

            // 2. On récupère le dump
            MemorySegment lengthPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment dumpPtr = filededup_h.FDDump(fdState, lengthPtr);
            int length = lengthPtr.get(ValueLayout.JAVA_INT, 0);

            if (length == 0 || dumpPtr.equals(MemorySegment.NULL)) {
                return S@@tream.empty();
            }

            // 3. Extraction sécurisée du char** (Tableau de pointeurs)
            MemorySegment sizedDump = dumpPtr.reinterpret((long) length * ValueLayout.ADDRESS.byteSize());
            List<String> currentGroup = new ArrayList<>();
            
            for (int i = 0; i < length; i++) {
                MemorySegment stringPtr = sizedDump.getAtIndex(ValueLayout.ADDRESS, i);
                
                // Le C sépare les groupes par des pointeurs NULL
                if (stringPtr.equals(MemorySegment.NULL)) {
                    if (currentGroup.size() > 1) {
                        allDuplicateGroups.add(new ArrayList<>(currentGroup));
                    }
                    currentGroup.clear();
                } else {
                    currentGroup.add(stringPtr.reinterpret(Long.MAX_VALUE).getString(0));
                }
            }
        }
        
        return allDuplicateGroups.stream();
    }

    @Override
    public String checkDuplicate(VirtualFileSystem vfs, String incomingPath) {
        // La logique C actuelle permet d'injecter un fichier et de parser le dump
        return null; // A faire pour l'équipe Storage
    }
}
```


#### Short summary: 

empty definition using pc, found symbol in pc: java/util/stream/Stream#