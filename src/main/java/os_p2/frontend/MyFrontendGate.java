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
 * C'est la porte d'entrée de notre application pour le Frontend.
 * Elle reçoit des requêtes JSON, les valide, et renvoie les résultats du scan.
 */
public class MyFrontendGate implements FrontendGate {

    private final VirtualFileSystem vfs;
    private final EngineRouter engineRouter;
    private final Gson gson;

    public MyFrontendGate(VirtualFileSystem vfs, EngineRouter engineRouter) {
        this.vfs = vfs;
        this.engineRouter = engineRouter;
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    /**
     * Reçoit une requête JSON et renvoie tout d'un coup dans un gros string JSON.
     */
    @Override
    @SuppressWarnings("unchecked")
    public String accept(String jsonString) {
        try {
            DedupRequestDto request = parseAndValidate(jsonString);

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
     * Pareil que accept(), mais renvoie un Stream pour traiter les données petit à petit.
     * C'est mieux pour les performances si on a des milliers de doublons.
     */
    @Override
    public Stream<String> acceptStream(String jsonString) {
        try {
            DedupRequestDto request = parseAndValidate(jsonString);
            return processRequestStream(request);
            
        } catch (IllegalArgumentException | JsonSyntaxException e) {
            return Stream.of(sendError(e.getMessage()));
        }
    }

    /**
     * Vérifie que le JSON reçu contient bien tout ce qu'il faut (action, scan_type, path, user).
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
     * Appelle le bon moteur de déduplication et transforme le résultat en JSON.
     */
    private Stream<String> processRequestStream(DedupRequestDto request) {
        try {
            System.out.println("[Frontend] Délégation du scan type '" + request.scan_type() + "' sur '" + request.path() + "' pour '" + request.user() + "'");
            
            var engine = engineRouter.getEngine(request.scan_type());
            
            return engine.scan(vfs, request.path(), request.user())
                .map(filePaths -> gson.toJson(filePaths));
            
        } catch (IllegalArgumentException e) {
            System.err.println("[Frontend] Erreur : " + e.getMessage());
            return Stream.empty();
        }
    }

    /**
     * Formate une réponse de succès.
     */
    private String sendSuccess(List<List<String>> groups) {
        return gson.toJson(new DedupResponseDto("success", groups));
    }

    /**
     * Formate une réponse d'erreur.
     */
    private String sendError(String errorMessage) {
        return gson.toJson(new DedupResponseDto("error", List.of()));
    }
}