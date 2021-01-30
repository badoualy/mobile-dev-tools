import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

task("runAnnotator", JavaExec::class) {
    main = "com.github.badoualy.mobileflow.annotator.AnnotatorKt"
    classpath = sourceSets["main"].runtimeClasspath
}

group = "com.github.badoualy"
version = "1.0.0"

dependencies {
    implementation("com.squareup.moshi:moshi:1.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")
    implementation("com.sksamuel.scrimage:scrimage-core:4.0.15")
    implementation("com.sksamuel.scrimage:scrimage-filters:4.0.15")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:4.0.15")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
