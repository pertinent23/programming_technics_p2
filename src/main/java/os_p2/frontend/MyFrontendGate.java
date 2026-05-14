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
 * Point d'entrée de l'API pour les requêtes JSON provenant du Frontend.
 * Valide les entrées et délègue le traitement aux moteurs de déduplication appropriés.
 */
public class MyFrontendGate implements FrontendGate {

    private final VirtualFileSystem vfs;
    private final EngineRouter engineRouter;
    private final Gson gson;

    /**
     * Initialise la porte d'entrée avec le VFS et le routeur de moteurs.
     * 
     * @param vfs Le système de fichiers virtuel.
     * @param engineRouter Le routeur pour sélectionner le moteur de déduplication.
     */
    public MyFrontendGate(VirtualFileSystem vfs, EngineRouter engineRouter) {
        this.vfs = vfs;
        this.engineRouter = engineRouter;
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    /**
     * Traite une requête JSON et retourne une réponse complète formatée.
     * 
     * @param jsonString La requête JSON brute.
     * @return La réponse JSON contenant les groupes de doublons ou une erreur.
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
     * Traite une requête JSON et retourne un flux de réponses (streaming).
     * 
     * @param jsonString La requête JSON brute.
     * @return Un flux de chaînes JSON représentant chaque groupe de doublons.
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
     * Analyse et valide la structure de la requête JSON entrante.
     * 
     * @param jsonString Le JSON brut.
     * @return Le DTO de la requête validée.
     * @throws IllegalArgumentException Si des paramètres obligatoires manquent ou sont invalides.
     * @throws NullPointerException Si le JSON est vide.
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
     * Délègue le traitement à l'Engine approprié et formate le flux de sortie.
     * 
     * @param request La requête DTO validée.
     * @return Un flux de chemins de fichiers doublons encodés en JSON.
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
     * Génère un JSON de succès pour la réponse complète.
     * 
     * @param groups Liste des groupes de doublons.
     * @return Chaîne JSON formatée.
     */
    private String sendSuccess(List<List<String>> groups) {
        return gson.toJson(new DedupResponseDto("success", groups));
    }

    /**
     * Génère un JSON d'erreur formaté.
     * 
     * @param errorMessage Le message d'erreur à inclure (optionnel dans le DTO actuel).
     * @return Chaîne JSON d'erreur.
     */
    private String sendError(String errorMessage) {
        return gson.toJson(new DedupResponseDto("error", List.of()));
    }
}