# ==========================================
# INFO0027 - Submission Automation Makefile
# Multi-Build Edition (Maven & Gradle)
# ==========================================

# Variables de configuration
ZIP_MAVEN = submission_maven.zip
ZIP_GRADLE = submission_gradle.zip
PDF_NAME = report/report.pdf
SRC_DIR  = src/

.PHONY: all check_pdf mvn gradle clean test_zip_mvn test_zip_gradle

# La cible par défaut si on tape juste "make"
all:
	@echo "=========================================================="
	@echo "Générateur de soumission Gradescope - Multi-Build"
	@echo "=========================================================="
	@echo "Commandes disponibles :"
	@echo "  make mvn      : Crée une archive propre avec MAVEN"
	@echo "  make gradle   : Crée une archive propre avec GRADLE"
	@echo "  make clean    : Nettoie les fichiers compilés et les ZIP"

# 1. Sécurité : Vérifie que le rapport PDF existe
check_pdf:
	@echo "📄 [1/4] Vérification de la présence du rapport PDF..."
	@if [ ! -f $(PDF_NAME) ]; then \
		echo "❌ ERREUR CRITIQUE: Le fichier $(PDF_NAME) est introuvable !"; \
		exit 1; \
	fi

# 2. Cible pour soumettre avec MAVEN
mvn: check_pdf
	@echo "🚀 [2/4] Préparation de la soumission Maven..."
	@echo "📦 [3/4] Création de l'archive $(ZIP_MAVEN)..."
	@rm -f $(ZIP_MAVEN)
	@cp $(PDF_NAME) report.pdf
	@# Archive UNIQUEMENT src, pom.xml et le pdf
	zip -r -q $(ZIP_MAVEN) $(SRC_DIR) pom.xml report.pdf
	@rm report.pdf
	@echo "✅ [4/4] Archive $(ZIP_MAVEN) générée avec succès !"
	@$(MAKE) test_zip_mvn

# 3. Cible pour soumettre avec GRADLE
gradle: check_pdf
	@echo "🚀 [2/4] Préparation de la soumission Gradle..."
	@echo "📦 [3/4] Création de l'archive $(ZIP_GRADLE)..."
	@rm -f $(ZIP_GRADLE)
	@cp $(PDF_NAME) report.pdf
	@# Archive UNIQUEMENT src, les fichiers gradle et le pdf
	zip -r -q $(ZIP_GRADLE) $(SRC_DIR) build.gradle.kts settings.gradle.kts gradlew gradlew.bat gradle/ report.pdf
	@rm report.pdf
	@echo "✅ [4/4] Archive $(ZIP_GRADLE) générée avec succès !"
	@$(MAKE) test_zip_gradle

# 4. Affiche un résumé du contenu de l'archive pour rassurer le développeur
test_zip_mvn:
	@echo "🔍 Vérification du contenu à la racine de l'archive Maven :"
	@unzip -l $(ZIP_MAVEN) | grep -E "pom.xml|report.pdf|$(SRC_DIR)" | head -n 5
	@echo "✨ Tout est prêt pour la soumission sur Gradescope (J6Y4N3) en version Maven !"

test_zip_gradle:
	@echo "🔍 Vérification du contenu à la racine de l'archive Gradle :"
	@unzip -l $(ZIP_GRADLE) | grep -E "build.gradle|report.pdf|$(SRC_DIR)" | head -n 5
	@echo "✨ Tout est prêt pour la soumission sur Gradescope (J6Y4N3) en version Gradle !"

# 5. Nettoyage du projet
clean:
	@echo "🧹 Nettoyage du projet..."
	@-./gradlew clean 2>/dev/null || true
	@-mvn clean 2>/dev/null || true
	rm -f $(ZIP_MAVEN) $(ZIP_GRADLE) report.pdf Deduplicator.jar