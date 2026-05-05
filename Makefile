# ==========================================
# INFO0027 - Submission Automation Makefile
# ==========================================

# Variables de configuration
ZIP_NAME = submission.zip
JAR_NAME = Deduplicator.jar
PDF_NAME = report/report.pdf
SRC_DIR  = src/

# Fichiers racine potentiels pour Gradle (pour que le prof puisse recompiler)
GRADLE_FILES = settings.gradle.kts gradlew gradlew.bat gradle/

.PHONY: all build check_pdf submit clean test_zip

# La cible par défaut si on tape juste "make"
all: submit

# 1. Recompile le projet pour avoir un JAR frais
build:
	@echo "🚀 [1/4] Construction du Fat JAR avec Gradle..."
	./gradlew clean build

# 2. Sécurité : Vérifie que le rapport PDF existe
check_pdf:
	@echo "📄 [2/4] Vérification de la présence du rapport PDF..."
	@if [ ! -f $(PDF_NAME) ]; then \
		echo "❌ ERREUR CRITIQUE: Le fichier $(PDF_NAME) est introuvable à la racine !"; \
		exit 1; \
	fi

# 3. Création de l'archive finale
submit: build check_pdf
	@echo "📦 [3/4] Création de l'archive $(ZIP_NAME)..."
	@rm -f $(ZIP_NAME)
	@# Copie le PDF à la racine temporairement pour qu'il soit à la racine dans le ZIP
	@cp $(PDF_NAME) report.pdf
	@# On zippe le JAR, le PDF, le dossier app, et les fichiers Gradle racine (s'ils existent)
	zip -r -q $(ZIP_NAME) $(JAR_NAME) report.pdf $(SRC_DIR) $$(ls -d $(GRADLE_FILES) 2>/dev/null)
	@# Nettoie le PDF temporaire
	@rm report.pdf
	@echo "✅ [4/4] Archive $(ZIP_NAME) générée avec succès !"
	@$(MAKE) test_zip

# 4. Affiche un résumé du contenu de l'archive pour rassurer le développeur
test_zip:
	@echo "🔍 Vérification du contenu à la racine de l'archive :"
	@unzip -l $(ZIP_NAME) | grep -E "$(JAR_NAME)|report.pdf|$(SRC_DIR)" | head -n 5
	@echo "✨ Tout est prêt pour la soumission sur Gradescope (J6Y4N3) !"

# 5. Nettoyage du projet
clean:
	@echo "🧹 Nettoyage du projet..."
	./gradlew clean
	rm -f $(ZIP_NAME) $(JAR_NAME)