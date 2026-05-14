package os_p2.frontend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import be.uliege.info0027.deduplication.VirtualFileSystem;
import os_p2.engine.EngineRouter;
import java.util.stream.Stream;

/**
 * Tests pour la passerelle Frontend.
 * Vérifie la validation des JSON et la gestion des erreurs.
 */
public class MyFrontendGateTest {

    @Test
    void testAcceptStreamInvalidJson() {
        VirtualFileSystem mockVfs = mock(VirtualFileSystem.class);
        EngineRouter realRouter = new EngineRouter();
        MyFrontendGate gate = new MyFrontendGate(mockVfs, realRouter);
        
        // JSON invalide (action incorrecte)
        String request = "{\"action\":\"wrong_action\", \"scan_type\":\"exact\", \"path\":\"/\", \"user\":\"alice\"}";
        
        Stream<String> result = gate.acceptStream(request);
        String response = result.findFirst().orElse("");
        
        assertTrue(response.contains("error"), "Une erreur devrait être renvoyée pour une action non supportée");
    }

    @Test
    void testAcceptEmptyJson() {
        VirtualFileSystem mockVfs = mock(VirtualFileSystem.class);
        EngineRouter realRouter = new EngineRouter();
        MyFrontendGate gate = new MyFrontendGate(mockVfs, realRouter);
        
        // La méthode accept() est conçue pour ne pas laisser sortir d'exception
        // et renvoyer un JSON d'erreur à la place.
        String response = gate.accept(null);
        
        assertNotNull(response);
        assertTrue(response.contains("error"), "Le système doit renvoyer un JSON d'erreur si l'entrée est nulle");
    }
}
