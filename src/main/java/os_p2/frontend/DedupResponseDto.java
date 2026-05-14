package os_p2.frontend;

import java.util.List;

/**
 * C'est l'objet qui représente la réponse renvoyée au Frontend.
 */
public record DedupResponseDto(
    String status,              // "success" ou "error"
    List<List<String>> groups   // La liste des groupes de doublons trouvés
) {}