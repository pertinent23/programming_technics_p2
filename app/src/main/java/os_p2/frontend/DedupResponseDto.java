package os_p2.frontend;

import java.util.List;

/**
 * Design Pattern : Data Transfer Object (DTO).
 * Modélise la réponse globale attendue par la méthode accept().
 */
public record DedupResponseDto(
    String status,              // "success" ou "error: [message]"
    List<List<String>> groups   // Liste des groupes de doublons trouvés
) {}