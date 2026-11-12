import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    id("com.gradleup.shadow") version "8.3.5"
    application
}

application {
    mainClass.set("com.bartixw.anymc.servers.MainKt")
}

dependencies {
    implementation(projects.anymcCore)
    implementation(projects.anymcMinecraftCore)
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.bartixw.anymc.servers.MainKt"
    }
}

val shadowJarTask = tasks.withType<ShadowJar> {
    mergeServiceFiles()

    archiveFileName.set("anymc-servers.jar")

    manifest {
        attributes("Main-Class" to "com.bartixw.anymc.servers.MainKt")
    }
}