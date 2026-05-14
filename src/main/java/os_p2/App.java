package os_p2;

import java.util.Collections;
import java.util.List;

import be.uliege.info0027.deduplication.FrontendGate;
import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Petite classe pour lancer l'appli manuellement et voir si tout compile et s'initialise bien.
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Démarrage du test manuel de l'application...");

        MyDeduplicationBootstrap bootstrap = new MyDeduplicationBootstrap();
        
        VirtualFileSystem mockVfs = new VirtualFileSystem() {
            @Override
            public List<VirtualFileInfo> listContent() {
                return Collections.emptyList();
            }

            @Override
            public List<VirtualFileInfo> listContent(String path, String user) {
                return Collections.emptyList();
            }
        };
        
        try {
            bootstrap.initialize(mockVfs); 
            
            FrontendGate gate = bootstrap.getFrontendGate();
            
            System.out.println("Le FrontendGate a bien été créé : " + (gate != null));
            System.out.println("Architecture prête et validée !");
            
        } catch (Exception e) {
            System.err.println("Le test a échoué : " + e.getMessage());
        }
    }

    public String getGreeting() {
        return "Bienvenue dans le projet de déduplication de fichiers !";
    }
}