import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

ktlint {
    version.set(libs.versions.ktlint)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

dependencies {
    implementation(libs.kotlin.csv)
    implementation(libs.sqlite.jdbc)

    testImplementation(libs.junit)

    // Shared detekt config contains the Compose rule set, so the plugin must be present.
    detektPlugins(libs.compose.rules.detekt)
}

val repoRoot = rootProject.layout.projectDirectory
val importerArgs = listOf(
    "--input",
    repoRoot.file("data/raw/verbs.csv").asFile.path,
    "--overrides",
    repoRoot.file("data/overrides.csv").asFile.path,
    "--ids",
    repoRoot.file("data/verb-ids.csv").asFile.path,
    "--selection",
    repoRoot.file("data/selection.csv").asFile.path,
    "--report",
    repoRoot.file("data/import-report.md").asFile.path,
    "--db",
    repoRoot.file("app/src/main/assets/verbs.db").asFile.path,
)

val utf8Output = listOf("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")

tasks.register<JavaExec>("importVerbs") {
    group = "glagolik"
    description = "Regenerates verb-ids.csv, selection.csv, import-report.md and verbs.db."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.eleasarb.glagolik.importer.MainKt")
    jvmArgs(utf8Output)
    args(importerArgs)
}

tasks.register<JavaExec>("verifyVerbs") {
    group = "verification"
    description = "Fails if the committed verb data is not what the importer produces."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("io.github.eleasarb.glagolik.importer.MainKt")
    jvmArgs(utf8Output)
    args(importerArgs + "--verify")
}

tasks.test {
    // ./gradlew :tools:importer:test -PupdateSnapshots=true rewrites the snapshot file.
    systemProperty(
        "updateSnapshots",
        providers.gradleProperty("updateSnapshots").getOrElse("false"),
    )
}
