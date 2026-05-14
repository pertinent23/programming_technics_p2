package os_p2.frontend;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import be.uliege.info0027.deduplication.FrontendGate;
import be.uliege.info0027.deduplication.VirtualFileSystem;
import os_p2.engine.EngineRouter;

/**
 * Implémentation du point d'entrée API (Design Pattern : Facade / Front Controller).
 * Responsabilités :
 * 1. Désérialisation et validation stricte des requêtes entrantes.
 * 2. Délégation du traitement au domaine métier (via Streams pour la performance).
 * 3. Formatage de la réponse selon le standard imposé par l'équipe Frontend.
 */
public class MyFrontendGate implements FrontendGate {

    private final VirtualFileSystem vfs;
    private final EngineRouter engineRouter;
    private final Gson gson;

    public MyFrontendGate(VirtualFileSystem vfs, EngineRouter engineRouter) {
        this.vfs = vfs;
        this.engineRouter = engineRouter;
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
        DedupRequestDto request = Objects.requireNonNull(
            gson.fromJson(jsonString, DedupRequestDto.class), 
            "Requête JSON vide..."
        );
        
        if (!"scan_duplicates".equals(request.action())) {
            throw new IllegalArgumentException("Action non supportée ou manquante. Seule 'scan_duplicates' est acceptée.");
        }
        if (request.scan_type() == null) {
            throw new IllegalArgumentException("Le paramètre 'scan_type' est manquant.");
        }
        if (request.path() == null) {
            throw new IllegalArgumentException("Le paramètre 'path' est manquant.");
        }
        if (request.user() == null || request.user().isBlank()) {
            throw new IllegalArgumentException("Le paramètre 'user' est manquant.");
        }
        
        return request;
    }

    /**
     * Méthode interne générant le flux de données. 
     * Délègue au moteur de déduplication approprié via la Factory (EngineRouter).
     */
    private Stream<String> processRequestStream(DedupRequestDto request) {
        try {
            System.out.println("[Frontend] Délégation du scan type '" + request.scan_type() + "' sur '" + request.path() + "' pour '" + request.user() + "'");
            
            // Récupérer le moteur approprié (Factory Pattern)
            var engine = engineRouter.getEngine(request.scan_type());
            
            // Scanner le chemin demandé et transformer les groupes de doublons en JSON
            return engine.scan(vfs, request.path(), request.user())
                .map(filePaths -> gson.toJson(filePaths));
            
        } catch (IllegalArgumentException e) {
            // Le moteur n'a pas pu être trouvé pour ce type de scan
            System.err.println("[Frontend] Erreur : " + e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Utilitaires de formatage des réponses JSON
     */
    private String sendSuccess(List<List<String>> groups) {
        return gson.toJson(new DedupResponseDto("success", groups));
    }

    private String sendError(String errorMessage) {
        // En cas d'erreur, on renvoie une liste vide de groupes pour respecter la structure
        return gson.toJson(new DedupResponseDto("error", List.of()));
    }
}