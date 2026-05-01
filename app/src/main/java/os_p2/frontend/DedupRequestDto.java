package os_p2.frontend;

/**
 * Design Pattern : Data Transfer Object (DTO).
 * Modélise la structure immuable de la requête entrante spécifiée p.2 du brief.
 * L'utilisation d'un record garantit la sécurité en environnement concurrent (thread-safe).
 */
public record DedupRequestDto(
    String action,     // "scan_duplicates"
    String scan_type,  // "exact" ou "similar"
    String path,       // Le chemin racine à scanner (ex: "/documents")
    String user        // L'utilisateur ayant initié la requête
) {}