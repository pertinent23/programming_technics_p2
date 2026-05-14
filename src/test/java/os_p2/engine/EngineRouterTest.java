package os_p2.engine;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Tests pour le routeur de moteurs.
 * Vérifie que le bon moteur est sélectionné et que le fallback fonctionne.
 */
public class EngineRouterTest {

    @Test
    void testGetSimilarEngine() {
        EngineRouter router = new EngineRouter();
        DeduplicationEngine engine = router.getEngine("similar");
        assertTrue(engine instanceof ImageSimilarityEngine);
    }

    @Test
    void testGetExactEngine() {
        EngineRouter router = new EngineRouter();
        DeduplicationEngine engine = router.getEngine("exact");
        // Devrait être soit LegacyNativeEngine, soit PureJavaHashEngine (fallback)
        assertNotNull(engine);
        assertTrue(engine instanceof LegacyNativeEngine || engine instanceof PureJavaHashEngine);
    }

    @Test
    void testInvalidEngine() {
        EngineRouter router = new EngineRouter();
        assertThrows(IllegalArgumentException.class, () -> {
            router.getEngine("invalid_type");
        });
    }
}
