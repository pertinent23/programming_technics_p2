/**
 * @file filededup.c
 * @brief Implémentation du détecteur de fichiers dupliqués (Projet INFO0027).
 * * @note Pour maximiser les performances, j'ai implémenté un "Pipeline d'élimination".
 * Au lieu de comparer bêtement tous les fichiers, je les filtre étape par étape :
 * 1. La taille (très rapide, via stat)
 * 2. Les inodes (pour détecter les hard links)
 * 3. Un pré-hachage (je ne lis que les extrémités du fichier)
 * 4. Un hachage complet (avec FNV-1a optimisé)
 * 5. Une comparaison byte-à-byte (seulement s'il y a un doute final)
 */

#define _GNU_SOURCE // J'active ceci pour pouvoir utiliser des fonctions Linux comme pread
#define _POSIX_C_SOURCE 200809L

#include "filededup.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <stdbool.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>

/* --- MES SEUILS D'OPTIMISATION --- */
#define SIZE_TABLE_CAPACITY 65536 // La taille de ma table de hachage principale
#define PREHASH_SIZE 8192         // Je lis 8 Ko maximum (4K au début, 4K à la fin) pour le pré-filtre
#define STOCHASTIC_THRESHOLD (50 * 1024 * 1024) // Si le fichier dépasse 50 Mo, j'échantillonne

/** * @enum FileState
 * J'utilise cet enum pour savoir où en est mon fichier dans le pipeline.
 * Ça m'évite de recalculer un hash si je l'ai déjà fait (Lazy Evaluation).
 */
typedef enum {
    STATE_NEW,        // Le fichier vient d'être ajouté, je n'ai encore rien lu
    STATE_PREHASHED,  // J'ai déjà calculé le pré-hash (les extrémités)
    STATE_FULLHASHED  // J'ai calculé le hash complet
} FileState;

/**
 * @struct FileNode
 * Ma structure pour représenter un fichier et stocker ses métadonnées.
 */
struct FileNode {
    char *filepath;       // Le chemin vers le fichier (ex: "dossier/test.txt")
    ino_t inode;          // L'identifiant physique du fichier sur le disque
    dev_t dev;            // L'identifiant de la partition du disque
    uint64_t pre_hash;    // Mon empreinte partielle (calculée rapidement)
    uint64_t full_hash;   // Mon empreinte complète
    FileState state;      // L'état d'avancement de ce fichier
};

/**
 * @struct SizeGroup
 * Je regroupe ici tous les fichiers qui ont exactement la même taille.
 * C'est la base de mon algorithme : des tailles différentes = fichiers différents.
 */
struct SizeGroup {
    off_t size;               // La taille commune de ces fichiers
    int count;                // Combien j'ai de fichiers dans ce groupe
    int capacity;             // La capacité de mon tableau (pour le realloc)
    struct FileNode *files;   // Mon tableau dynamique de fichiers
    struct SizeGroup *next;   // Pointeur pour gérer les collisions dans ma table de hachage
};

/**
 * @struct magic
 * C'est l'état global de mon détecteur (l'ADT opaque pour l'utilisateur).
 */
struct magic {
    struct SizeGroup **size_buckets; // Mon tableau de listes (table de hachage)
};

/* ========================================================================= */
/* --- MES FONCTIONS DE HACHAGE ET COMPARAISON --- */
/* ========================================================================= */

/**
 * @brief Mon algorithme de hachage (FNV-1a 64 bits).
 * @details J'ai choisi FNV-1a au lieu de la Tabulation car il est beaucoup plus rapide.
 * Il multiplie et fait des XOR, ce que le processeur fait en un seul cycle d'horloge.
 */
static inline uint64_t fast_hash64(const uint8_t *data, size_t length) {
    // Ce sont les constantes mathématiques officielles de FNV-1a
    uint64_t hash = 14695981039346656037ULL; 
    const uint64_t prime = 1099511628211ULL; 
    
    // Je parcours mes données octet par octet pour les mélanger dans mon hash
    for (size_t i = 0; i < length; ++i) {
        hash = (hash ^ data[i]) * prime;
    }
    return hash;
}

/**
 * @brief Mon premier filtre : je lis juste le début et la fin du fichier.
 * @details Si deux fichiers sont différents, ça se voit souvent dès l'en-tête (header).
 * En ne lisant que 8 Ko, j'économise énormément de temps sur les gros fichiers.
 */
static void compute_pre_hash(struct FileNode *node, off_t size) {
    if (node->state >= STATE_PREHASHED) return; // Si c'est déjà fait, je quitte

    int fd = open(node->filepath, O_RDONLY);
    if (fd < 0) return; // Sécurité : si je n'ai pas les droits de lecture

    uint8_t buffer[PREHASH_SIZE];
    
    if (size <= PREHASH_SIZE) {
        // Si le fichier est plus petit que 8Ko, je le lis en entier d'un coup
        ssize_t r = read(fd, buffer, (size_t)size);
        node->pre_hash = (r > 0) ? fast_hash64(buffer, (size_t)r) : 0;
    } else {
        // Sinon, je lis 4Ko au début...
        ssize_t b1 = read(fd, buffer, 4096);
        // ...et 4Ko à la toute fin (pread permet de lire sans déplacer le curseur)
        ssize_t b2 = pread(fd, buffer + 4096, 4096, size - 4096);
        
        size_t total = (size_t)((b1 > 0 ? b1 : 0) + (b2 > 0 ? b2 : 0));
        node->pre_hash = (total > 0) ? fast_hash64(buffer, total) : 0;
    }
    
    close(fd);
    node->state = STATE_PREHASHED; // Je met à jour l'état
}

/**
 * @brief Je calcule ici l'empreinte complète pour les fichiers vraiment suspects.
 */
static void compute_full_hash(struct FileNode *node, off_t size) {
    if (node->state >= STATE_FULLHASHED) return;

    int fd = open(node->filepath, O_RDONLY);
    if (fd < 0) return;

    // Si le fichier est gigantesque (> 50 Mo), je fais du hachage par échantillons
    if (size > STOCHASTIC_THRESHOLD) {
        size_t chunk_size = 1024 * 1024; // Je prends des blocs de 1 Mo
        uint8_t *chunk_buf = malloc(chunk_size * 5);
        if (chunk_buf) {
            // Je prélève 5 échantillons : 0%, 25%, 50%, 75% et 100% du fichier
            off_t offsets[5] = {0, size/4, size/2, (size*3)/4, size - (off_t)chunk_size};
            size_t total = 0;
            for (int k = 0; k < 5; k++) {
                ssize_t r = pread(fd, chunk_buf + total, chunk_size, offsets[k]);
                if (r > 0) total += (size_t)r;
            }
            node->full_hash = fast_hash64(chunk_buf, total);
            free(chunk_buf);
        }
    } else {
        // Pour les fichiers normaux, j'utilise mmap.
        // Cela mappe le fichier en RAM sans passer par les buffers classiques du C.
        void *map = mmap(NULL, (size_t)size, PROT_READ, MAP_PRIVATE, fd, 0);
        if (map != MAP_FAILED) {
            // Je dis à Linux de pré-charger le fichier car je vais le lire en séquence
            posix_madvise(map, (size_t)size, POSIX_MADV_SEQUENTIAL); 
            node->full_hash = fast_hash64((const uint8_t *)map, (size_t)size);
            munmap(map, (size_t)size);
        }
    }
    close(fd);
    node->state = STATE_FULLHASHED;
}

/**
 * @brief Mon test de certitude absolue : je compare octet par octet.
 */
static bool are_files_identical(const struct FileNode *f1, const struct FileNode *f2, off_t size) {
    if (size == 0) return true; // Deux fichiers de 0 octet sont forcément identiques
    
    // ASTUCE INODE : Si c'est le même fichier physique (hard link), j'ai gagné en O(1) !
    if (f1->inode == f2->inode && f1->dev == f2->dev) return true;

    int fd1 = open(f1->filepath, O_RDONLY);
    int fd2 = open(f2->filepath, O_RDONLY);
    if (fd1 < 0 || fd2 < 0) { 
        if (fd1 >= 0) close(fd1); 
        if (fd2 >= 0) close(fd2); 
        return false; 
    }

    // Je mappe les deux fichiers en mémoire
    void *m1 = mmap(NULL, (size_t)size, PROT_READ, MAP_PRIVATE, fd1, 0);
    void *m2 = mmap(NULL, (size_t)size, PROT_READ, MAP_PRIVATE, fd2, 0);
    bool same = false;
    
    if (m1 != MAP_FAILED && m2 != MAP_FAILED) {
        // memcmp est imbattable pour comparer deux blocs mémoire rapidement
        same = (memcmp(m1, m2, (size_t)size) == 0);
    }
    
    // Nettoyage obligatoire pour ne pas fuir la mémoire
    if (m1 != MAP_FAILED) munmap(m1, (size_t)size);
    if (m2 != MAP_FAILED) munmap(m2, (size_t)size);
    close(fd1); 
    close(fd2);
    
    return same;
}

/* ========================================================================= */
/* --- MON API PUBLIQUE --- */
/* ========================================================================= */

/**
 * @brief J'initialise mon détecteur.
 */
FILEDEDUP FDInit(void) {
    struct magic *fd = malloc(sizeof(struct magic));
    if (!fd) return NULL; 

    // calloc est pratique car il met toutes mes cases à NULL par défaut
    fd->size_buckets = calloc(SIZE_TABLE_CAPACITY, sizeof(struct SizeGroup *));
    if (!fd->size_buckets) { free(fd); return NULL; }
    
    return fd;
}

/**
 * @brief J'ajoute un fichier dans ma structure (sans le lire !).
 */
int FDCheck(FILEDEDUP fd, char *filepath) {
    // CLAUSE DE SÉCURITÉ : Je vérifie que l'utilisateur ne m'envoie pas de pointeur NULL
    if (fd == NULL || filepath == NULL) {
        return 0; 
    }

    struct stat st;
    // J'utilise stat() pour avoir la taille et les infos sans ouvrir le fichier
    if (stat(filepath, &st) != 0 || !S_ISREG(st.st_mode)) {
        return 0; // Si le fichier n'existe pas ou est un dossier, je l'ignore
    }

    off_t size = st.st_size;
    
    // HACHAGE DE FIBONACCI : Je multiplie par le nombre d'or pour bien disperser 
    // mes tailles de fichiers dans mon tableau et éviter les bouchons.
    size_t idx = ((uint64_t)size * 11400714819323198485ULL) % SIZE_TABLE_CAPACITY;

    // Je cherche si un groupe avec cette taille exacte existe déjà
    struct SizeGroup *sg = fd->size_buckets[idx];
    while (sg != NULL && sg->size != size) {
        sg = sg->next;
    }

    // Si je ne le trouve pas, je crée un nouveau groupe
    if (sg == NULL) {
        sg = malloc(sizeof(struct SizeGroup));
        if (!sg) return 0;
        sg->size = size;
        sg->count = 0;
        sg->capacity = 8; // Je commence avec un tableau de taille 8
        sg->files = malloc((size_t)sg->capacity * sizeof(struct FileNode));
        sg->next = fd->size_buckets[idx];
        fd->size_buckets[idx] = sg;
    }

    // Si mon tableau est plein, je double sa taille (Tableau dynamique)
    if (sg->count == sg->capacity) {
        sg->capacity *= 2;
        sg->files = realloc(sg->files, (size_t)sg->capacity * sizeof(struct FileNode));
    }

    // Je sauvegarde les infos de base. Le hash sera calculé plus tard.
    sg->files[sg->count].filepath = strdup(filepath);
    sg->files[sg->count].inode = st.st_ino;
    sg->files[sg->count].dev = st.st_dev;
    sg->files[sg->count].state = STATE_NEW;
    sg->count++;

    return 1;
}

/**
 * @brief Ma fonction utilitaire pour ajouter des résultats à mon tableau final.
 */
static void append_to_result(char ***res_array, int *cap, int *len, char *str) {
    if (*len >= *cap) {
        *cap = (*cap == 0) ? 16 : (*cap * 2);
        *res_array = realloc(*res_array, (size_t)(*cap) * sizeof(char *));
    }
    // Si on me passe NULL, c'est pour séparer les groupes de doublons
    (*res_array)[*len] = str ? strdup(str) : NULL;
    (*len)++;
}

/**
 * @brief C'est ici que je déclenche mon analyse paresseuse (Lazy Evaluation).
 */
char **FDDump(FILEDEDUP fd, int *length) {
    // Sécurité contre les pointeurs nuls pour éviter un Segmentation Fault
    if (fd == NULL) {
        if (length != NULL) *length = 0;
        return NULL;
    }
    if (length == NULL) return NULL;

    char **result = NULL;
    int cap = 0, len = 0;

    // Je parcours tous les groupes de mon tableau principal
    for (size_t b = 0; b < SIZE_TABLE_CAPACITY; b++) {
        struct SizeGroup *sg = fd->size_buckets[b];
        while (sg != NULL) {
            // S'il n'y a qu'un seul fichier de cette taille, il n'a pas de doublon.
            // Je passe directement à la suite. C'est mon plus grand gain de temps !
            if (sg->count > 1) {
                
                // J'applique mon Filtre 1 : Le Pré-hachage pour tout le groupe
                for (int i = 0; i < sg->count; i++) {
                    compute_pre_hash(&sg->files[i], sg->size);
                }

                // Ce tableau va me servir à me souvenir des fichiers déjà groupés
                bool *matched = calloc((size_t)sg->count, sizeof(bool));
                
                for (int i = 0; i < sg->count; i++) {
                    if (matched[i]) continue;
                    
                    bool cluster_started = false;
                    // Je compare le fichier 'i' avec tous les fichiers suivants 'j'
                    for (int j = i + 1; j < sg->count; j++) {
                        if (matched[j]) continue;

                        // ÉTAPE 1 : Si le pré-hash est différent, je rejette direct
                        if (sg->files[i].pre_hash != sg->files[j].pre_hash) continue;

                        // ÉTAPE 2 : Si le pré-hash est identique, je lance le hachage lourd
                        compute_full_hash(&sg->files[i], sg->size);
                        compute_full_hash(&sg->files[j], sg->size);

                        if (sg->files[i].full_hash == sg->files[j].full_hash) {
                            // ÉTAPE 3 : Si les hash complets sont identiques, je fais mon memcmp
                            if (are_files_identical(&sg->files[i], &sg->files[j], sg->size)) {
                                if (!cluster_started) {
                                    append_to_result(&result, &cap, &len, sg->files[i].filepath);
                                    cluster_started = true;
                                }
                                append_to_result(&result, &cap, &len, sg->files[j].filepath);
                                matched[j] = true;
                            }
                        }
                    }
                    // Si j'ai trouvé un ou plusieurs doublons, je clôture le groupe avec un NULL
                    if (cluster_started) {
                        append_to_result(&result, &cap, &len, NULL); 
                    }
                }
                free(matched); // Je n'oublie pas de libérer ma mémoire temporaire
            }
            sg = sg->next;
        }
    }

    // S'il n'y a aucun doublon du tout, l'énoncé me demande de renvoyer NULL
    if (len == 0) {
        *length = 0;
        return NULL;
    }

    // Je m'assure que le dernier élément de mon tableau de chaînes est bien un NULL
    if (result[len - 1] != NULL) {
        append_to_result(&result, &cap, &len, NULL);
    }

    *length = len;
    return result;
}