/**
 * @file filededup.h
 * @brief Interface pour le détecteur de fichiers dupliqués.
 * @note Implémenté selon les spécifications INFO0027 - Project 1.
 */

#ifndef FILEDEDUP_H
#define FILEDEDUP_H

/* Le type FILEDEDUP est un pointeur opaque vers la structure interne 'magic'  */
typedef struct magic *FILEDEDUP;

/**
 * @brief Initialise la structure de données du détecteur.
 * @return Une instance allouée de FILEDEDUP, ou NULL en cas d'erreur.
 */
FILEDEDUP FDInit(void);

/**
 * @brief Ajoute le contenu du fichier au détecteur.
 * @param fd L'instance FILEDEDUP.
 * @param filepath Le chemin d'accès vers le fichier à analyser.
 * @return 1 en cas de succès, 0 en cas d'erreur (fichier inaccessible, etc.).
 */
int FDCheck(FILEDEDUP fd, char *filepath);

/**
 * @brief Retourne tous les ensembles de fichiers identiques.
 * @param fd L'instance FILEDEDUP.
 * @param length Paramètre de retour contenant la taille du tableau renvoyé.
 * @return Un tableau de chaînes de caractères (chemins), séparés et terminés par NULL.
 */
char **FDDump(FILEDEDUP fd, int *length);

#endif /* FILEDEDUP_H */