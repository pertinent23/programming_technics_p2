plugins {
    application
    java
    id("com.gradleup.shadow") version "9.3.0"
}

repositories {
    mavenCentral()
    
    // Dépôt GitLab institutionnel ULiège (Fourni par le professeur)
    maven {
        url = uri("https://gitlab.uliege.be/api/v4/projects/8360/packages/maven")
        name = "GitLab ULiège"
        credentials(HttpHeaderCredentials::class) {
            name = findProperty("gitLabTokenName") as String?
            value = findProperty("gitLabPrivateToken") as String?
        }
        authentication {
            create("header", HttpHeaderAuthentication::class)
        }
    }
}

dependencies {
    // Dépendances de test (existantes dans ton fichier d'origine)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Ta dépendance Guava
    implementation(libs.guava)

    // Dépendance vers les interfaces fournies par tes profs
    implementation("be.uliege.info0027:filededup-interfaces:1.0.0")

    //Pour gérer le format json
    implementation("com.google.code.gson:gson:2.10.1")

    implementation("org.openpnp:opencv:4.7.0-0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// Configuration pour nommer le fichier final exactement comme demandé
tasks.named<Jar>("shadowJar") {
    archiveFileName.set("Deduplicator.jar")
    destinationDirectory.set(rootProject.layout.projectDirectory)
    outputs.upToDateWhen { false }
    outputs.cacheIf { false }
}

// Pour que la tâche de build standard génère aussi le Shadow JAR
tasks.named("build") {
    dependsOn("shadowJar")
}

application {
    // Définit la classe principale (assure-toi que c'est le bon chemin vers ton App)
    mainClass.set("os_p2.App")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("java.library.path", file("libs").absolutePath)
}

tasks.withType<Test> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("java.library.path", file("libs").absolutePath)
}