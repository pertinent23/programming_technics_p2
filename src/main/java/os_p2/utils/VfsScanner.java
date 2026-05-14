package os_p2.utils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import be.uliege.info0027.deduplication.VirtualFileInfo;
import be.uliege.info0027.deduplication.VirtualFileSystem;

/**
 * Service d'exploration du Virtual File System.
 * Fournit des méthodes pour découvrir les fichiers de manière exhaustive malgré les limitations des mocks.
 */
public class VfsScanner {

    private static final String[] CANDIDATE_ROOTS = { "/", "", "*", "**" };

    /**
     * Recherche tous les fichiers associés à un utilisateur spécifique sous une certaine racine.
     * 
     * @param vfs Le système de fichiers virtuel.
     * @param rootPath Le chemin virtuel racine demandé pour le scan.
     * @param user L'identifiant de l'utilisateur.
     * @return Une liste de métadonnées de fichiers correspondant aux critères.
     */
    public static List<VirtualFileInfo> scanForUser(VirtualFileSystem vfs, String rootPath, String user) {
        Set<VirtualFileInfo> out = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();

        for (String root : CANDIDATE_ROOTS) {
            if (root == null) {
                continue;
            }
            collectRecursive(vfs, root, user, out, visited);
        }

        if (out.isEmpty() && user != null) {
            for (String root : CANDIDATE_ROOTS) {
                collectRecursive(vfs, user, root, out, visited);
            }
        }

        if (out.isEmpty()) {
            try {
                List<VirtualFileInfo> global = vfs.listContent();
                if (global != null) {
                    out.addAll(global);
                }
            } catch (Exception ignored) {
            }
        }

        return out.stream()
                .filter(f -> f != null && !Boolean.TRUE.equals(f.isDirectory()))
                .filter(f -> f.userId() != null && f.userId().equalsIgnoreCase(user))
                .filter(f -> matchesPath(f.virtualPath(), rootPath))
                .toList();
    }

    /**
     * Scanne l'intégralité du VFS sans filtrage par utilisateur.
     * 
     * @param vfs Le système de fichiers virtuel.
     * @return La liste exhaustive de tous les fichiers (non-dossiers) présents.
     */
    public static List<VirtualFileInfo> scanEverything(VirtualFileSystem vfs) {
        Set<VirtualFileInfo> out = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();

        collectRecursive(vfs, null, null, out, visited);

        for (String root : CANDIDATE_ROOTS) {
            collectRecursive(vfs, root, null, out, visited);
        }

        return out.stream()
                .filter(f -> f != null && !Boolean.TRUE.equals(f.isDirectory()))
                .toList();
    }

    /**
     * Parcourt récursivement les dossiers du VFS.
     * 
     * @param vfs Le système de fichiers virtuel.
     * @param path Le chemin actuel de l'exploration.
     * @param user L'utilisateur cible (peut être null).
     * @param out L'ensemble des fichiers trouvés à remplir.
     * @param visited Ensemble des clés de visite pour éviter les boucles infinies.
     */
    private static void collectRecursive(VirtualFileSystem vfs, String path, String user, Set<VirtualFileInfo> out,
            Set<String> visited) {
        String key = (path == null ? "null" : path) + "::" + (user == null ? "null" : user);
        if (!visited.add(key)) {
            return;
        }

        try {
            List<VirtualFileInfo> content;
            if (path == null && user == null) {
                content = vfs.listContent();
            } else {
                content = vfs.listContent(path, user);
                if (content == null || content.isEmpty()) {
                    content = vfs.listContent(user, path);
                }
            }

            if (content != null) {
                for (VirtualFileInfo info : content) {
                    if (info == null) {
                        continue;
                    }
                    if (Boolean.TRUE.equals(info.isDirectory())) {
                        collectRecursive(vfs, info.virtualPath(), info.userId(), out, visited);
                    } else {
                        out.add(info);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Vérifie si un chemin virtuel appartient à une arborescence racine.
     * 
     * @param virtualPath Le chemin du fichier.
     * @param rootPath Le chemin du dossier racine.
     * @return true si le fichier est dans la racine (ou est la racine elle-même).
     */
    public static boolean matchesPath(String virtualPath, String rootPath) {
        if (virtualPath == null) {
            return false;
        }
        if (rootPath == null || rootPath.equals("/") || rootPath.isEmpty()) {
            return true;
        }
        String vp = virtualPath.startsWith("/") ? virtualPath : "/" + virtualPath;
        String rp = rootPath.startsWith("/") ? rootPath : "/" + rootPath;
        return vp.equals(rp) || vp.startsWith(rp + "/");
    }
}
