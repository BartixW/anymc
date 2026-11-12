plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":anymc-core"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            
            // For now, we might need NBT if we move NBT related logic
            implementation("com.viaversion:nbt:5.0.0")
        }
        
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }
}
