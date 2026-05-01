package os_p2.engine;

/**
 * Design Pattern : Factory.
 * Instancie et distribue le bon moteur en fonction de la requête (exact ou similar).
 * Implémente la logique de "Seamless Swap" demandée dans le brief (Fallback Java pur).
 */
public class EngineRouter {

    /**
     * Fournit le moteur approprié selon le type de scan demandé par le frontend.
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
     * Tente de charger le moteur C optimisé. S'il échoue (problème d'OS, librairie manquante),
     * bascule de manière transparente sur l'implémentation Java pure.
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