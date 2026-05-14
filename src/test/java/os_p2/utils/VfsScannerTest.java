package os_p2.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;
import java.util.List;

/**
 * Tests unitaires pour le scanner VFS.
 * Utilise directement le Record VirtualFileInfo fourni par la librairie.
 */
public class VfsScannerTest {

    @Test
    void testScanForUserDiscovery() {
        VirtualFileSystem mockVfs = mock(VirtualFileSystem.class);
        
        // Instanciation directe du Record avec le bon ordre des paramètres :
        // (virtualPath, isDirectory, size, userId, physicalPath)
        VirtualFileInfo file1 = new VirtualFileInfo("/docs/notes.txt", false, 100L, "alice", "/tmp/p1");
        
        // Simulation du comportement du VFS
        when(mockVfs.listContent("/", "alice")).thenReturn(List.of(file1));

        List<VirtualFileInfo> results = VfsScanner.scanForUser(mockVfs, "/", "alice");

        // Vérification
        assertFalse(results.isEmpty(), "Le scanner devrait trouver au moins un fichier");
        assertEquals("/docs/notes.txt", results.get(0).virtualPath());
        assertEquals("alice", results.get(0).userId());
    }
}
