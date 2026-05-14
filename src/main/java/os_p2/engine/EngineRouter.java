package os_p2.engine;

/**
 * C'est ici qu'on choisit quel moteur utiliser (Exact ou Similar).
 * C'est aussi ici qu'on gère le "Seamless Swap" pour passer du C au Java si besoin.
 */
public class EngineRouter {

    /**
     * Renvoie le bon moteur selon ce que le frontend a demandé.
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
     * Tente d'utiliser le moteur C, mais si ça plante (librairie pas là, etc.),
     * on bascule sur le moteur Java sans que l'utilisateur ne s'en rende compte.
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