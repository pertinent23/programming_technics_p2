package os_p2.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;
import java.util.List;

/**
 * Tests pour le cache d'indexation par taille.
 */
public class VfsIndexCacheTest {

    @Test
    void testCacheIndexing() {
        VirtualFileSystem mockVfs = mock(VirtualFileSystem.class);
        
        // Utilisation directe du Record VirtualFileInfo (virtualPath, isDirectory, size, userId, physicalPath)
        VirtualFileInfo f1 = new VirtualFileInfo("/a.txt", false, 100L, "u1", "/p1");
        VirtualFileInfo f2 = new VirtualFileInfo("/b.txt", false, 200L, "u1", "/p2");
        VirtualFileInfo f3 = new VirtualFileInfo("/c.txt", false, 100L, "u1", "/p3");
        
        when(mockVfs.listContent()).thenReturn(List.of(f1, f2, f3));
        
        VfsIndexCache cache = new VfsIndexCache(mockVfs);
        
        List<VirtualFileInfo> candidates100 = cache.getCandidatesBySize(100L);
        assertEquals(2, candidates100.size(), "Il devrait y avoir 2 fichiers de 100 octets");
        
        List<VirtualFileInfo> candidates200 = cache.getCandidatesBySize(200L);
        assertEquals(1, candidates200.size());
        
        List<VirtualFileInfo> candidatesEmpty = cache.getCandidatesBySize(500L);
        assertTrue(candidatesEmpty.isEmpty());
    }
}
