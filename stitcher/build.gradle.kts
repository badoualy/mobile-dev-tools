import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

repositories {
    jcenter()
}

task("runStitcher", JavaExec::class) {
    main = "com.github.badoualy.mobile.stitcher.StitcherKt"
    classpath = sourceSets["main"].runtimeClasspath
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation("com.sksamuel.scrimage:scrimage-core:4.0.15")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}
