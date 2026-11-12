import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.compose.ComposePlugin.DesktopDependencies.currentOs
import java.util.Locale
import java.lang.management.ManagementFactory

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

val osName = System.getProperty("os.name").lowercase(Locale.ROOT)

sourceSets {
    main {
        resources {
            val platformDir = if (osName.contains("win")) "windows" else "linux"
            srcDir("src/nativeResources/$platformDir")
            srcDir(layout.buildDirectory.dir("nativeResources"))
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(projects.anymcCore)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation("io.github.vinceglb:filekit-compose:0.8.8")

    // LWJGL 3.3.5 for GraalVM compatibility
    val lwjglVersion = "3.3.5"
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
}

compose.desktop {
    application {
        mainClass = "com.bartixw.anymc.installer.MainKt"
        jvmArgs += listOf(
            "--enable-native-access=ALL-UNNAMED",
            "--add-opens=java.base/sun.misc=ALL-UNNAMED",
            "--add-exports=java.base/sun.misc=ALL-UNNAMED"
        )

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "AnyMC Installer"
            packageVersion = "0.1.0"
            vendor = "BartixW"

            modules("jdk.unsupported")

            windows {
                iconFile.set(project.file("../desktopApp/icon/app_icon.ico"))
                upgradeUuid = "a123b456-c789-4000-8000-123456789abc"
                shortcut = true
                menu = true
            }
        }
    }
}

compose.resources {
    publicResClass = true
}

val shadowJarTask = tasks.withType<ShadowJar> {
    mergeServiceFiles()

    val destination = file("${rootDir}/build/app")
    destinationDirectory.set(destination)

    archiveFileName.set("AnyMC-Installer.jar")

    manifest {
        attributes("Main-Class" to "com.bartixw.anymc.installer.MainKt")
    }
}

graalvmNative {
    toolchainDetection.set(false)

    binaries.all {
        imageName.set("AnyMC-Installer")

        buildArgs.add("-H:Class=com.bartixw.anymc.installer.MainKt")
        buildArgs.add("-Djava.library.path=.")
        buildArgs.add("--no-fallback")
        buildArgs.add("--enable-preview")
        buildArgs.add("-H:+UnlockExperimentalVMOptions")
        buildArgs.add("-H:+JNI")

        buildArgs.add("--initialize-at-build-time=kotlin")
        buildArgs.add("--initialize-at-build-time=sun.awt.OSInfo,sun.awt.OSInfo$1")

        buildArgs.add("--initialize-at-run-time=org.lwjgl")
        buildArgs.add("--initialize-at-run-time=com.bartixw.anymc")
        buildArgs.add("--initialize-at-run-time=org.jetbrains.skia,org.jetbrains.skiko")
        buildArgs.add("--initialize-at-run-time=java.awt,sun.awt,javax.swing,sun.java2d,sun.font")
        buildArgs.add("--initialize-at-run-time=com.sun.java.swing.plaf.windows")

        buildArgs.add("--report-unsupported-elements-at-runtime")
        buildArgs.add("--trace-class-initialization=org.lwjgl.system.ThreadLocalUtil,kotlin.reflect.KVariance,kotlin.DeprecationLevel")

        buildArgs.add("-H:+AllowIncompleteClasspath")
        buildArgs.add("--enable-native-access=ALL-UNNAMED")

        buildArgs.add("-Dorg.lwjgl.system.allocator=system")
        buildArgs.add("-Dorg.lwjgl.system.noFastPath=true")
        buildArgs.add("-Dorg.lwjgl.librarypath=lib")

        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        buildArgs.add("-Djava.home=.")
        buildArgs.add("-J-Djava.home=$javaHome")

        val platformDir = when {
            osName.contains("win") -> "windows"
            else -> "linux"
        }
        val metaInfDir = file("src/nativeResources/$platformDir/META-INF/native-image")

        val jniConfigPath = file("$metaInfDir/jni-config.json").absolutePath
        if (file(jniConfigPath).exists()) {
            buildArgs.add("-H:JNIConfigurationFiles=$jniConfigPath")
        }

        val reflectConfigPath = file("$metaInfDir/reflect-config.json").absolutePath
        if (file(reflectConfigPath).exists()) {
            buildArgs.add("-H:ReflectionConfigurationFiles=$reflectConfigPath")
        }

        val proxyConfigPath = file("$metaInfDir/proxy-config.json").absolutePath
        if (file(proxyConfigPath).exists()) {
            buildArgs.add("-H:DynamicProxyConfigurationFiles=$proxyConfigPath")
        }

        val resourceConfigPath = file("$metaInfDir/resource-config.json").absolutePath
        if (file(resourceConfigPath).exists()) {
            buildArgs.add("-H:ResourceConfigurationFiles=$resourceConfigPath")
        }

        val serializationConfigPath = file("$metaInfDir/serialization-config.json").absolutePath
        if (file(serializationConfigPath).exists()) {
            buildArgs.add("-H:SerializationConfigurationFiles=$serializationConfigPath")
        }

        buildArgs.add("-H:IncludeResources=.*skiko.*")
        buildArgs.add("-H:IncludeResources=.*native_libs/.*")
        buildArgs.add("-H:IncludeResources=.*composeResources/.*")
        buildArgs.add("-H:IncludeResources=.*\\.(cvr|cvr2)$")
        buildArgs.add("-H:IncludeResources=.*\\.(sha256|sha1|so|dll|dylib)$")
        buildArgs.add("-H:IncludeResources=.*anymc\\.(installerapp|shared)\\.generated\\.resources.*$")
        buildArgs.add("-H:IncludeResources=.*swing.*")
        buildArgs.add("-H:IncludeResources=.*com/sun/swing/.*")
        buildArgs.add("-H:IncludeResourceBundles=com.sun.swing.internal.plaf.basic.resources.basic")
        buildArgs.add("-H:IncludeResourceBundles=com.sun.swing.internal.plaf.metal.resources.metal")

        when {
            osName.contains("win") -> {
                buildArgs.add("-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS")
                buildArgs.add("-H:NativeLinkerOption=/ENTRY:mainCRTStartup")
                buildArgs.add("-H:NativeLinkerOption=/LIBPATH:$javaHome/lib")
                buildArgs.add("-H:NativeLinkerOption=jawt.lib")
                buildArgs.add("-H:NativeLinkerOption=jvm.lib")
                buildArgs.add("-H:NativeLinkerOption=java.lib")
            }
            osName.contains("mac") -> {
                buildArgs.add("-H:NativeLinkerOption=-L$javaHome/lib")
                buildArgs.add("-H:NativeLinkerOption=-lawt")
                buildArgs.add("-H:NativeLinkerOption=-lawt_lwawt")
                buildArgs.add("-H:NativeLinkerOption=-Wl,-rpath,@executable_path/lib")
                buildArgs.add("-H:NativeLinkerOption=-Wl,-rpath,@executable_path")
            }
            else -> {
                buildArgs.add("-H:NativeLinkerOption=-L$javaHome/lib")
                buildArgs.add("-H:NativeLinkerOption=-L$javaHome/lib/headless")
                buildArgs.add("-H:NativeLinkerOption=-L$javaHome/lib/xawt")
                buildArgs.add("-H:NativeLinkerOption=-lawt")
                buildArgs.add("-H:NativeLinkerOption=-lawt_xawt")
                buildArgs.add("-H:NativeLinkerOption=-ljawt")
                buildArgs.add("-H:NativeLinkerOption=-ljava")
                buildArgs.add("-H:NativeLinkerOption=-ljvm")
                buildArgs.add("-H:NativeLinkerOption=-Wl,-rpath,'\$ORIGIN/lib:\$ORIGIN'")
            }
        }

        val totalPhysicalMemory = try {
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val method = osBean::class.java.getMethod("getTotalPhysicalMemorySize")
            method.invoke(osBean) as Long
        } catch (_: Exception) {
            0L
        }

        val heapSize = if (totalPhysicalMemory > 0) {
            val targetBytes = (totalPhysicalMemory * 0.75).toLong()
            val targetGb = (targetBytes / (1024 * 1024 * 1024)).coerceIn(2, 8)
            "${targetGb}g"
        } else {
            "4g"
        }
        jvmArgs.add("-Xmx$heapSize")
    }
}

val packageNativeInstaller = tasks.register<Copy>("packageNativeInstaller") {
    description = "Copies compiled single-executable native installer to build/app/"
    group = "build"

    dependsOn("nativeCompile")

    val srcDir = layout.buildDirectory.dir("native/nativeCompile")
    from(srcDir) {
        include("AnyMC-Installer", "AnyMC-Installer.exe")
    }

    into(file("${rootDir}/build/app"))
}

val prepareNativeResources = tasks.register<Copy>("prepareNativeResources") {
    duplicatesStrategy = DuplicatesStrategy.WARN

    val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
    val libDir = file("$javaHome/lib")

    val ext = when {
        osName.contains("win") -> "dll"
        osName.contains("mac") -> "dylib"
        else -> "so"
    }

    val essentialBaseNames = listOf(
        "jvm", "awt", "awt_xawt", "awt_headless", "awt_lwawt", "jawt", "java", "nio", "net", "verify", "fontmanager", "management", "management_ext"
    )
    val essentialLibs = essentialBaseNames.map { "lib$it.$ext" } + essentialBaseNames.map { "$it.$ext" }

    from(libDir) {
        include(essentialLibs)
    }

    if (osName.contains("linux")) {
        from("$libDir/server") { include("libjvm.so"); into("lib/server") }
        from("$libDir/xawt") { include("libawt_xawt.so"); into("lib/xawt") }
        from("$libDir/headless") { include("libawt_headless.so"); into("lib/headless") }
        from(libDir) { include("libjvm.so", "libawt.so", "libjawt.so", "libjava.so", "libfontmanager.so"); into("lib") }
    }

    if (osName.contains("win")) {
        from("$javaHome/bin") {
            include(essentialLibs)
            include("jvm.dll", "management.dll", "management_ext.dll")
            into("bin")
        }
        from("$javaHome/bin/server") {
            include("jvm.dll")
            into("bin/server")
        }
    }

    val nativeConfig = configurations.runtimeClasspath.get()
    nativeConfig.filter { it.name.contains("lwjgl") && it.name.endsWith(".jar") }.forEach { jarFile ->
        from(zipTree(jarFile)) {
            include("**/*.$ext")
            eachFile { path = name }
            includeEmptyDirs = false
        }
    }

    into(layout.buildDirectory.dir("nativeResources/native_libs"))
}

tasks.named("processResources") {
    dependsOn(prepareNativeResources)
}

tasks.named("nativeCompile") {
    dependsOn(prepareNativeResources)
}

tasks.named("build") {
    dependsOn(packageNativeInstaller)
}

tasks.named("nativeCompile") {
    dependsOn(prepareNativeResources)
}

tasks.named("build") {
    dependsOn(packageNativeInstaller)
}
