package os_p2.utils;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests unitaires pour l'utilitaire FileIO.
 * On vérifie que le hachage et la comparaison binaire fonctionnent bien.
 */
public class FileIOTest {

    @TempDir
    Path tempDir;

    @Test
    void testCalculateSha256() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Hello World");
        
        String hash = FileIO.calculateSha256(file);
        
        assertNotNull(hash);
        // Hash SHA-256 connu pour "Hello World"
        assertEquals("a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e", hash);
    }

    @Test
    void testAreIdentical() throws Exception {
        Path file1 = tempDir.resolve("file1.bin");
        Path file2 = tempDir.resolve("file2.bin");
        Path file3 = tempDir.resolve("file3.bin");

        Files.write(file1, new byte[]{1, 2, 3});
        Files.write(file2, new byte[]{1, 2, 3});
        Files.write(file3, new byte[]{1, 2, 4});

        assertTrue(FileIO.areIdentical(file1, file2), "Les fichiers identiques devraient matcher");
        assertFalse(FileIO.areIdentical(file1, file3), "Les fichiers différents ne devraient pas matcher");
    }
}
