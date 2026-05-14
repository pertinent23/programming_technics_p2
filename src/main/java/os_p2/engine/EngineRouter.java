package os_p2.engine;

/**
 * Routeur et Factory pour les moteurs de déduplication.
 * Gère le choix entre les scans exacts et par similarité, ainsi que le mécanisme de repli.
 */
public class EngineRouter {

    /**
     * Fournit le moteur de déduplication approprié en fonction du type de scan demandé.
     * 
     * @param scanType Le type de scan ("exact" ou "similar").
     * @return Une instance de DeduplicationEngine prête à l'emploi.
     * @throws IllegalArgumentException Si le type de scan n'est pas supporté.
     */
    public DeduplicationEngine getEngine(String scanType) {
        if ("similar".equalsIgnoreCase(scanType)) {
            return new ImageSimilarityEngine();
        }
        
        if ("exact".equalsIgnoreCase(scanType)) {
            return getExactEngineWithSeamlessSwap();
        }

        throw new IllegalArgumentException("Type de scan non supporté : " + scanType);
    }

    /**
     * Tente de créer le moteur natif et bascule sur le moteur Java en cas d'indisponibilité.
     * 
     * @return Le meilleur moteur exact disponible (Natif ou Java).
     */
    private DeduplicationEngine getExactEngineWithSeamlessSwap() {
        try {
            return new LegacyNativeEngine();
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            System.err.println("[EngineRouter] WARN: Moteur natif C indisponible. Bascule sur Java pur.");
            return new PureJavaHashEngine();
        }
    }
}