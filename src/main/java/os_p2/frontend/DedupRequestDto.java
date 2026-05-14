package os_p2.frontend;

/**
 * C'est l'objet qui représente la question posée par le Frontend.
 */
public record DedupRequestDto(
    String action,     // Qu'est-ce qu'on doit faire ? (ex: scan_duplicates)
    String scan_type,  // Quel algorithme utiliser ? (ex: exact ou similar)
    String path,       // Quel dossier scanner ?
    String user        // Qui fait la demande ?
) {}