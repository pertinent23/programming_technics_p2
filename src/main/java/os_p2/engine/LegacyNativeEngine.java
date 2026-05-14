package os_p2.engine;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;
import os_p2.native_c.filededup_h;

/**
 * Ce moteur utilise la librairie C native pour aller plus vite.
 * On utilise l'API FFM (Foreign Function & Memory) pour appeler les fonctions C.
 */
public class LegacyNativeEngine implements DeduplicationEngine {

    static {
        System.loadLibrary("filededup");
    }

    /**
     * On scanne les fichiers d'un utilisateur en passant par le moteur C.
     * C'est beaucoup plus rapide pour les gros volumes de données.
     */
    @Override
    public Stream<List<String>> scan(VirtualFileSystem vfs, String rootPath, String user) {
        List<List<String>> allDuplicateGroups = new ArrayList<>();

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fdState = filededup_h.FDInit();
            if (fdState.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Erreur critique : FDInit a retourné NULL.");
            }

            Map<String, List<String>> physToVirt = new HashMap<>();

            List<be.uliege.info0027.deduplication.VirtualFileInfo> files = 
                os_p2.utils.VfsScanner.scanForUser(vfs, rootPath, user);

            for (VirtualFileInfo info : files) {
                if (Boolean.TRUE.equals(info.isDirectory())) {
                    continue;
                }
                if (info.physicalPath() == null) {
                    continue;
                }
                physToVirt.computeIfAbsent(info.physicalPath(), p -> new ArrayList<>()).add(info.virtualPath());
                MemorySegment cString = arena.allocateFrom(info.physicalPath());
                filededup_h.FDCheck(fdState, cString);
            }

            MemorySegment lengthPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment dumpPtr = filededup_h.FDDump(fdState, lengthPtr);
            int length = lengthPtr.get(ValueLayout.JAVA_INT, 0);

            if (length == 0 || dumpPtr.equals(MemorySegment.NULL)) {
                return Stream.empty();
            }

            MemorySegment sizedDump = dumpPtr.reinterpret((long) length * ValueLayout.ADDRESS.byteSize());
            List<String> currentGroup = new ArrayList<>();

            for (int i = 0; i < length; i++) {
                MemorySegment stringPtr = sizedDump.getAtIndex(ValueLayout.ADDRESS, i);
                if (stringPtr.equals(MemorySegment.NULL)) {
                    if (currentGroup.size() > 1) {
                        allDuplicateGroups.add(new ArrayList<>(currentGroup));
                    }
                    currentGroup.clear();
                } else {
                    String phys = stringPtr.reinterpret(Long.MAX_VALUE).getString(0);
                    List<String> virts = physToVirt.get(phys);
                    if (virts != null) {
                        for (String v : virts) {
                            if (!currentGroup.contains(v)) {
                                currentGroup.add(v);
                            }
                        }
                    }
                }
            }
            if (currentGroup.size() > 1) {
                allDuplicateGroups.add(new ArrayList<>(currentGroup));
            }
        }

        return allDuplicateGroups.stream();
    }

    /**
     * Vérifie si un fichier physique est déjà connu du moteur C.
     */
    @Override
    public String checkDuplicate(VirtualFileSystem vfs, String incomingPath) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment fdState = filededup_h.FDInit();
            if (fdState.equals(MemorySegment.NULL)) {
                return null;
            }

            if (!Files.isRegularFile(Path.of(incomingPath))) {
                return null;
            }

            Map<String, String> physToVirt = new HashMap<>();

            for (VirtualFileInfo info : vfs.listContent()) {
                if (Boolean.TRUE.equals(info.isDirectory())) {
                    continue;
                }
                if (info.physicalPath() == null) {
                    continue;
                }
                physToVirt.put(info.physicalPath(), info.virtualPath());
                MemorySegment cString = arena.allocateFrom(info.physicalPath());
                filededup_h.FDCheck(fdState, cString);
            }

            MemorySegment cStringIncoming = arena.allocateFrom(incomingPath);
            filededup_h.FDCheck(fdState, cStringIncoming);

            MemorySegment lengthPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment dumpPtr = filededup_h.FDDump(fdState, lengthPtr);
            int length = lengthPtr.get(ValueLayout.JAVA_INT, 0);

            if (length == 0 || dumpPtr.equals(MemorySegment.NULL)) {
                return null;
            }

            MemorySegment sizedDump = dumpPtr.reinterpret((long) length * ValueLayout.ADDRESS.byteSize());
            List<String> currentGroup = new ArrayList<>();

            for (int i = 0; i < length; i++) {
                MemorySegment stringPtr = sizedDump.getAtIndex(ValueLayout.ADDRESS, i);
                if (stringPtr.equals(MemorySegment.NULL)) {
                    if (currentGroup.contains(incomingPath) && currentGroup.size() > 1) {
                        currentGroup.remove(incomingPath);
                        return physToVirt.get(currentGroup.getFirst());
                    }
                    currentGroup.clear();
                } else {
                    currentGroup.add(stringPtr.reinterpret(Long.MAX_VALUE).getString(0));
                }
            }
            if (currentGroup.contains(incomingPath) && currentGroup.size() > 1) {
                currentGroup.remove(incomingPath);
                return physToVirt.get(currentGroup.getFirst());
            }
        } catch (Exception e) {
            System.err.println("[LegacyNativeEngine] Erreur : " + e.getMessage());
        }
        return null;
    }
}