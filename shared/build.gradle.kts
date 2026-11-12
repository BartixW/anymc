plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":anymc-core"))
            implementation(project(":anymc-minecraft-core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.markdown.renderer)
            implementation("com.viaversion:nbt:5.0.0")
            implementation("io.github.vinceglb:filekit-compose:0.8.8")

            val jmeVersion = "3.6.1-stable"
            implementation("org.jmonkeyengine:jme3-core:${jmeVersion}")
            implementation("org.jmonkeyengine:jme3-desktop:${jmeVersion}")
            implementation("org.jmonkeyengine:jme3-lwjgl3:${jmeVersion}") {
                exclude(group = "org.lwjgl", module = "lwjgl-jemalloc")
            }
            implementation("org.jmonkeyengine:jme3-plugins:${jmeVersion}")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}

compose.resources {
    publicResClass = true
}