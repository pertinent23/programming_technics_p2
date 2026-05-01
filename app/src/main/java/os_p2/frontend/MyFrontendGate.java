package os_p2.frontend;

import java.util.List;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import be.uliege.info0027.deduplication.FrontendGate;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Implémentation du point d'entrée API (Design Pattern : Facade / Front Controller).
 * Responsabilités :
 * 1. Désérialisation et validation stricte des requêtes entrantes.
 * 2. Délégation du traitement au domaine métier (via Streams pour la performance).
 * 3. Formatage de la réponse selon le standard imposé par l'équipe Frontend.
 */
public class MyFrontendGate implements FrontendGate {

    private final VirtualFileSystem vfs;
    private final Gson gson;

    public MyFrontendGate(VirtualFileSystem vfs) {
        this.vfs = vfs;
        // On désactive l'échappement HTML pour garder des chemins propres (ex: évite les \u0026)
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    /**
     * Traite la requête et renvoie la réponse complète d'un coup.
     * Réutilise intelligemment la logique de streaming pour éviter la duplication de code.
     */
    @Override
    @SuppressWarnings("unchecked")
    public String accept(String jsonString) {
        try {
            DedupRequestDto request = parseAndValidate(jsonString);

            // On consomme le stream et on l'agrège en une liste finale
            List<List<String>> allGroups = processRequestStream(request)
                .map(jsonArray -> gson.fromJson(jsonArray, List.class))
                .map(list -> (List<String>) list)
                .toList();

            return sendSuccess(allGroups);

        } catch (IllegalArgumentException | JsonSyntaxException e) {
            return sendError(e.getMessage());
        } catch (Exception e) {
            return sendError("Erreur interne lors du traitement.");
        }
    }

    /**
     * Traite la requête de manière paresseuse (Lazy Evaluation).
     * Indispensable pour ne pas faire exploser la RAM si l'espace de stockage est immense.
     */
    @Override
    public Stream<String> acceptStream(String jsonString) {
        try {
            DedupRequestDto request = parseAndValidate(jsonString);
            return processRequestStream(request);
            
        } catch (IllegalArgumentException | JsonSyntaxException e) {
            // En streaming, si une erreur survient au lancement, on renvoie un objet JSON d'erreur formaté.
            return Stream.of(sendError(e.getMessage()));
        }
    }

    /**
     * Validation métier de la requête JSON.
     */
    private DedupRequestDto parseAndValidate(String jsonString) {
        DedupRequestDto request = gson.fromJson(jsonString, DedupRequestDto.class);
        
        if (request == null) {
            throw new IllegalArgumentException("Requête JSON vide ou mal formatée.");
        }
        if (!"scan_duplicates".equals(request.action())) {
            throw new IllegalArgumentException("Action non supportée ou manquante.");
        }
        if (request.scan_type() == null || request.path() == null) {
            throw new IllegalArgumentException("Paramètres 'scan_type' ou 'path' manquants.");
        }
        
        return request;
    }

    /**
     * Méthode interne générant le flux de données. 
     * C'est ici que s'interfacera le futur package 'engine'.
     */
    private Stream<String> processRequestStream(DedupRequestDto request) {
        // TODO: Lors du développement du package 'engine', instancier la Factory ici :
        // DeduplicationEngine engine = engineFactory.getEngine(request.scan_type());
        // Stream<DuplicateGroup> groups = engine.scan(vfs, request.path());
        // return groups.map(group -> gson.toJson(group.filePaths()));

        // Bouchon (Stub) temporaire pour tester le routing de manière isolée
        System.out.println("[Frontend] Délégation du scan type '" + request.scan_type() + "' sur '" + request.path() + "'");
        
        if ("exact".equals(request.scan_type())) {
            // Le brief demande un stream de tableaux JSON en strings
            return Stream.of(
                gson.toJson(List.of("/doc1.txt", "/backup/doc1.txt")),
                gson.toJson(List.of("/photo.jpg", "/archive/photo.jpg"))
            );
        }
        
        return Stream.empty();
    }

    /**
     * Utilitaires de formatage des réponses JSON
     */
    private String sendSuccess(List<List<String>> groups) {
        return gson.toJson(new DedupResponseDto("success", groups));
    }

    private String sendError(String errorMessage) {
        // En cas d'erreur, on renvoie une liste vide de groupes pour respecter la structure
        return gson.toJson(new DedupResponseDto("error: " + errorMessage, List.of()));
    }
}