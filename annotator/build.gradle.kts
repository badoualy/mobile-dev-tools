import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

repositories {
    jcenter()
}

task("runAnnotator", JavaExec::class) {
    main = "com.github.badoualy.mobile.annotator.AnnotatorKt"
    classpath = sourceSets["main"].runtimeClasspath
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation(project(":stitcher"))

    implementation("com.squareup.moshi:moshi:1.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.11.0")

    implementation("com.sksamuel.scrimage:scrimage-core:4.0.15")
    implementation("com.sksamuel.scrimage:scrimage-filters:4.0.15")
    implementation("com.sksamuel.scrimage:scrimage-formats-extra:4.0.15")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
