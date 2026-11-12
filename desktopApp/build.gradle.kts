import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.compose.ComposePlugin.DesktopDependencies.currentOs
import java.util.Locale
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Compression

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

dependencies {
    implementation(projects.shared)
    implementation(projects.anymcCore)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)

    // Force LWJGL 3.3.5 for GraalVM compatibility
    val lwjglVersion = "3.3.5"
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opencl:$lwjglVersion")
}

val assetsDir = layout.buildDirectory.dir("assets")
val authHelperOutputDir = layout.buildDirectory.dir("assets/authhelper")

compose.desktop {
    application {
        mainClass = "com.bartixw.anymc.MainKt"
        jvmArgs += listOf(
            "--enable-native-access=ALL-UNNAMED",
            "--add-opens=java.base/sun.misc=ALL-UNNAMED",
            "--add-exports=java.base/sun.misc=ALL-UNNAMED"
        )

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "AnyMC Launcher"
            packageVersion = "0.1.0"

            vendor = "BartixW"

            modules("jdk.unsupported")

            linux {
                iconFile.set(project.file("src/icon/app_icon.png"))
            }

            macOS {
                iconFile.set(project.file("src/icon/app_icon.ico"))
            }

            windows {
                iconFile.set(project.file("src/icon/app_icon.ico"))
                upgradeUuid = "f450b3e3-f680-4cf0-8d2c-da6b88cc710e"
                shortcut = true
                menu = true
            }

            appResourcesRootDir.set(assetsDir)
        }
    }
}

compose.resources {
    publicResClass = true
}

val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
val (runtimeId, exeName) = when {
    osName.contains("win") -> Pair("win-x64", "AnyMCAuthHelper.exe")
    osName.contains("mac") -> Pair("osx-x64", "AnyMCAuthHelper")
    else -> Pair("linux-x64", "AnyMCAuthHelper")
}

val authHelperDotnetOutputDir = file("../authHelper/bin/dist-$runtimeId")

val compileCSharpAuthHelper = tasks.register<Exec>("compileCSharpHelper") {
    description = "Compiles AuthHelper for current platform"
    group = "build"

    workingDir = file("../authHelper")

    commandLine("dotnet", "publish", "-c", "Release", "-r", runtimeId, "-o", authHelperDotnetOutputDir.absolutePath)


}

val copyCSharpAuthHelper = tasks.register<Copy>("copyCSharpHelper") {
    description = "Copies compiled AuthHelper to build folder"
    group = "build"

    dependsOn(compileCSharpAuthHelper)

    into(authHelperOutputDir)

    from(authHelperDotnetOutputDir) {
        include(exeName)

        include("**/*.dll")
        include("**/*.dylib")
        include("**/*.so")

        exclude("**/*.pdb", "**/*.dbg")
    }
}

tasks.configureEach {
    if (name == "run" || name == "hotRun" || name == "shadowJar" || name.contains("Distributable") || name.contains("Package")) {
        dependsOn(copyCSharpAuthHelper)
    }
}

tasks.matching { it.name.contains("Distributable") || it.name.contains("AppImage") || it.name.contains("Package") }.configureEach {
    doLast {
        val appBinariesDir = file("${layout.buildDirectory.get().asFile}/compose/binaries")
        if (appBinariesDir.exists()) {
            appBinariesDir.walkTopDown().forEach { f ->
                if (f.isDirectory && (f.name.equals("AnyMC Launcher", ignoreCase = true) || f.name.endsWith(".app"))) {
                    val targetAuthHelperDir = File(f, "authhelper")
                    targetAuthHelperDir.mkdirs()
                    file(authHelperOutputDir).copyRecursively(targetAuthHelperDir, overwrite = true)
                    println("Injected AuthHelper into distributable: ${targetAuthHelperDir.absolutePath}")
                }
            }
        }
    }
}

val copyExternalFiles = tasks.register<Copy>("copyExternalFiles") {
    description = "Copies external files into the build app/ directory"

    from(authHelperOutputDir) {
        include("*")
    }

    into(file("${rootDir}/build/app/authhelper"))
}

val shadowJarTask = tasks.withType<ShadowJar> {
    mergeServiceFiles()

    val destination = file("${rootDir}/build/app")

    destinationDirectory.set(destination)

    archiveFileName.set("AnyMC.jar")

    manifest {
        attributes("Main-Class" to "com.bartixw.anymc.MainKt")
    }

    finalizedBy(copyExternalFiles)
}

graalvmNative {
    toolchainDetection.set(false)

    binaries.all {
        imageName.set("anymc-native")

        buildArgs.add("-H:Class=com.bartixw.anymc.MainKt")
        buildArgs.add("-Djava.library.path=.")
        buildArgs.add("--no-fallback")
        buildArgs.add("--enable-preview")
        buildArgs.add("-H:+UnlockExperimentalVMOptions")
        buildArgs.add("-H:+JNI")

        // --- Refined Initialization Strategy ---
        // Broaden Kotlin build-time initialization to resolve build errors with enums like KVariance
        buildArgs.add("--initialize-at-build-time=kotlin")
        buildArgs.add("--initialize-at-build-time=sun.awt.OSInfo,sun.awt.OSInfo$1")

        // Strictly initialize renderer-related components at runtime to prevent segfaults
        buildArgs.add("--initialize-at-run-time=org.lwjgl")
        buildArgs.add("--initialize-at-run-time=com.jme3")
        buildArgs.add("--initialize-at-run-time=com.bartixw.anymc")
        buildArgs.add("--initialize-at-run-time=org.jetbrains.skia,org.jetbrains.skiko")
        buildArgs.add("--initialize-at-run-time=java.awt,sun.awt,javax.swing,sun.java2d,sun.font")
        buildArgs.add("--initialize-at-run-time=com.sun.java.swing.plaf.windows")

        buildArgs.add("--report-unsupported-elements-at-runtime")
        buildArgs.add("--trace-class-initialization=org.lwjgl.system.ThreadLocalUtil,kotlin.reflect.KVariance,kotlin.DeprecationLevel")

        // --- Added for Unsafe & JNI Access ---
        buildArgs.add("-H:+AllowIncompleteClasspath")
        buildArgs.add("--enable-native-access=ALL-UNNAMED")

        buildArgs.add("-Dorg.lwjgl.system.allocator=system")
        buildArgs.add("-Dorg.lwjgl.system.noFastPath=true")
        buildArgs.add("-Dorg.lwjgl.librarypath=lib")

        // Use the builder's own java.home for the build process to avoid crashes,
        // but set the image's java.home to '.' for runtime portability.
        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        buildArgs.add("-Djava.home=.")
        buildArgs.add("-J-Djava.home=$javaHome")

        val jniConfigPath = file("src/nativeResources/META-INF/native-image/jni-config.json").absolutePath
        if (file(jniConfigPath).exists()) {
            buildArgs.add("-H:JNIConfigurationFiles=$jniConfigPath")
        }

        val reflectConfigPath = file("src/nativeResources/META-INF/native-image/reflect-config.json").absolutePath
        if (file(reflectConfigPath).exists()) {
            buildArgs.add("-H:ReflectionConfigurationFiles=$reflectConfigPath")
        }

        val proxyConfigPath = file("src/nativeResources/META-INF/native-image/proxy-config.json").absolutePath
        if (file(proxyConfigPath).exists()) {
            buildArgs.add("-H:DynamicProxyConfigurationFiles=$proxyConfigPath")
        }

        val resourceConfigPath = file("src/nativeResources/META-INF/native-image/resource-config.json").absolutePath
        if (file(resourceConfigPath).exists()) {
            buildArgs.add("-H:ResourceConfigurationFiles=$resourceConfigPath")
        }

        val serializationConfigPath = file("src/nativeResources/META-INF/native-image/serialization-config.json").absolutePath
        if (file(serializationConfigPath).exists()) {
            buildArgs.add("-H:SerializationConfigurationFiles=$serializationConfigPath")
        }

        buildArgs.add("-H:IncludeResources=.*skiko.*")
        buildArgs.add("-H:IncludeResources=.*\\.(sha256|sha1|so|dll|dylib)$")
        buildArgs.add("-H:IncludeResources=.*anymc\\.(desktopapp|shared)\\.generated\\.resources.*$")
        buildArgs.add("-H:IncludeResources=.*swing.*")
        buildArgs.add("-H:IncludeResources=.*com/sun/swing/.*")
        buildArgs.add("-H:IncludeResources=.*\\.(j3md|frag|vert|glsllib|fnt|png|cfg|properties|glb|ttf)$")
        buildArgs.add("-H:IncludeResourceBundles=com.sun.swing.internal.plaf.basic.resources.basic")
        buildArgs.add("-H:IncludeResourceBundles=com.sun.swing.internal.plaf.metal.resources.metal")

        when (osName) {
            "win" -> {
                buildArgs.add("-H:NativeLinkerOption=/LIBPATH:$javaHome/lib")
                buildArgs.add("-H:NativeLinkerOption=awt.lib")
            }
            "osx" -> {
                buildArgs.add("-H:NativeLinkerOption=-L$javaHome/lib")
                buildArgs.add("-H:NativeLinkerOption=-lawt")
                buildArgs.add("-H:NativeLinkerOption=-lawt_lwawt")
                buildArgs.add("-H:NativeLinkerOption=-Wl,-rpath,@executable_path/lib")
                buildArgs.add("-H:NativeLinkerOption=-Wl,-rpath,@executable_path")
            }
            else -> { // Linux
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

        jvmArgs.add("-Xmx12g")
    }
}

val copyNativeLibs = tasks.register<Copy>("copyNativeLibs") {
    duplicatesStrategy = DuplicatesStrategy.WARN

    val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
    val libDir = file("$javaHome/lib")

    val ext = when (osName) {
        "win" -> "dll"
        "osx" -> "dylib"
        else -> "so"
    }

    // Essential libraries for AWT/Skiko to reduce bloat
    val essentialBaseNames = listOf(
        "awt", "awt_xawt", "awt_lwawt", "jawt", "java", "nio", "net", "verify", "fontmanager", "management", "management_ext"
    )
    val essentialLibs = essentialBaseNames.map { "lib$it.$ext" } + essentialBaseNames.map { "$it.$ext" }

    from(libDir) {
        include(essentialLibs)
    }

    // On Linux, these are often in subdirectories
    if (osName == "linux") {
        from("$libDir/xawt") { include("libawt_xawt.so"); into("xawt") }
        from("$libDir/headless") { include("libawt_headless.so"); into("headless") }
    }

    // On Windows, some essential DLLs are in bin/
    if (osName == "win") {
        from("$javaHome/bin") {
            include(essentialLibs)
            include("management.dll", "management_ext.dll")
        }
    }

    // LWJGL natives - they are in the classpath, but GraalVM might not handle resource extraction well.
    // We'll collect them from the configurations.
    val nativeConfig = configurations.runtimeClasspath.get()
    nativeConfig.filter { it.name.contains("lwjgl") && it.name.endsWith(".jar") }.forEach { jarFile ->
        from(zipTree(jarFile)) {
            include("**/*.$ext")
            eachFile {
                // Flatten the directory structure for LWJGL natives
                path = name
            }
            includeEmptyDirs = false
        }
    }

    into(layout.buildDirectory.dir("native/nativeCompile/lib"))
}

tasks.named("nativeCompile") {
    dependsOn(copyNativeLibs)
}

tasks.register<JavaExec>("runWithAgent") {
    group = "verification"
    description = "Runs the application with the GraalVM Native Image Agent to generate configuration."

    mainClass.set("com.bartixw.anymc.MainKt")
    classpath = sourceSets["main"].runtimeClasspath

    // Path to the agent output directory
    val agentDir = file("src/nativeResources/META-INF/native-image")

    jvmArgs("-agentlib:native-image-agent=config-merge-dir=${agentDir.absolutePath}")

    // Ensure native libs are available during the run
    val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
    systemProperty("java.library.path", file("$javaHome/bin").absolutePath)
}

val appLauncherDirProvider = layout.buildDirectory.dir("compose/binaries/main/app").map { parent ->
    parent.asFile.listFiles()?.firstOrNull { it.isDirectory } ?: parent.asFile
}

val packageDesktopZip = tasks.register<Zip>("packageDesktopZip") {
    group = "distribution"
    description = "Packages the desktopApp distributable into a ZIP archive (matching release.yml)."
    dependsOn("createDistributable")

    from(appLauncherDirProvider)

    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    val archiveName = when {
        osName.contains("mac") -> "AnyMC-macos-x64.zip"
        else -> "AnyMC-windows-x64.zip"
    }
    archiveFileName.set(archiveName)
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("dist"))
}

val packageDesktopTar = tasks.register<Tar>("packageDesktopTar") {
    group = "distribution"
    description = "Packages the desktopApp distributable into a tar.gz archive (matching release.yml)."
    dependsOn("createDistributable")

    from(appLauncherDirProvider)

    archiveFileName.set("AnyMC-linux-x64.tar.gz")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("dist"))
    compression = Compression.GZIP
}

val packageDesktopArchive = tasks.register("packageDesktopArchive") {
    group = "distribution"
    description = "Generates the appropriate .zip or .tar.gz archive for the current platform (matching release.yml)."
    val osName = System.getProperty("os.name").lowercase(Locale.ROOT)
    if (osName.contains("linux")) {
        dependsOn(packageDesktopTar)
    } else {
        dependsOn(packageDesktopZip)
    }
}
