/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft

import com.bartixw.anymc.minecraft.data.*
import com.bartixw.anymc.minecraft.utils.LaunchUtils
import java.io.File

object LaunchLogic {
    data class LibInfo(val path: String, val version: String?)

    fun buildClasspath(
        baseDir: File,
        clientData: Client,
        modLoaderData: Any?,
        currentOS: String,
        isFabric: Boolean,
        isForge: Boolean,
        mcVersion: String
    ): Pair<String, String> {
        val librariesDir = baseDir.resolve("libraries")
        val libraryMap = mutableMapOf<String, Library>()
        LaunchUtils.mergeLibraries(clientData, libraryMap)

        val unifiedLibraryMap = mutableMapOf<String, LibInfo>()

        fun addLibToUnifiedMap(library: Library) {
            val parts = library.name.split(":")
            if (parts.size >= 3) {
                val group = parts[0]
                val artifact = parts[1]
                val classifier = if (parts.size >= 4) parts[3] else ""
                val identity =
                    if (classifier.isNotEmpty()) "$group:$artifact:$classifier" else "$group:$artifact"
                val key = identity

                val artifactObj = library.downloads?.artifact
                val path = artifactObj?.path ?: LaunchUtils.libraryToPath(library.name)
                val file = librariesDir.resolve(path)

                if (!file.exists()) return

                val coordinate = LaunchUtils.parseMavenCoordinate(library.name)
                val currentLib = unifiedLibraryMap[key]

                if (currentLib == null) {
                    unifiedLibraryMap[key] = LibInfo(file.absolutePath, coordinate?.version)
                } else if (coordinate != null && currentLib.version != null) {
                    if (LaunchUtils.compareVersions(coordinate.version, currentLib.version) > 0) {
                        unifiedLibraryMap[key] = LibInfo(file.absolutePath, coordinate.version)
                    }
                }
            }
        }

        when (modLoaderData) {
            is FabricProfile -> modLoaderData.libraries.forEach { addLibToUnifiedMap(it) }
            is MultiMCProfile -> (modLoaderData.libraries + modLoaderData.mavenFiles).forEach {
                addLibToUnifiedMap(
                    it
                )
            }
        }

        libraryMap.values.forEach { addLibToUnifiedMap(it) }

        val jarFile = baseDir.resolve("${clientData.id}.jar")
        val isModularNeoForge = isForge && mcVersion.startsWith("1.21")

        return if (isModularNeoForge) {
            val modulePathKeywords = listOf(
                "bootstraplauncher",
                "securejarhandler",
                "org.ow2.asm"
            )

            val mpList = mutableListOf<String>()
            val cpList = mutableListOf<String>()

            unifiedLibraryMap.entries.forEach { entry ->
                val path = entry.value.path
                val isGameModule = entry.key.contains("minecraft-game-module") ||
                        entry.key.contains(":srg") ||
                        entry.key.contains(":universal") ||
                        entry.key.contains("JarJarFileSystems")

                if (!isGameModule && modulePathKeywords.any { entry.key.contains(it) }) {
                    mpList.add(path)
                } else {
                    cpList.add(path)
                }
            }

            if (jarFile.exists()) {
                cpList.add(jarFile.absolutePath)
            }

            Pair(
                cpList.distinct().joinToString(File.pathSeparator),
                mpList.distinct().joinToString(File.pathSeparator)
            )
        } else {
            val cp = (unifiedLibraryMap.values.map { it.path } + jarFile.absolutePath).distinct()
                .joinToString(File.pathSeparator)
            Pair(cp, "")
        }
    }

    fun extractNatives(baseDir: File, clientData: Client, currentOS: String) {
        val librariesDir = baseDir.resolve("libraries")
        val nativesDir = baseDir.resolve("natives")

        if (nativesDir.exists()) nativesDir.deleteRecursively()
        nativesDir.mkdirs()

        val libraryMap = mutableMapOf<String, Library>()
        LaunchUtils.mergeLibraries(clientData, libraryMap)

        libraryMap.values.forEach { library ->
            val nativeClassifier = library.natives?.get(currentOS)
                ?: if (library.name.contains("org.lwjgl") || library.name.startsWith("com.mojang:text2speech")) "natives-$currentOS" else null

            if (nativeClassifier != null) {
                val nativeArtifact = library.downloads?.classifiers?.get(nativeClassifier)
                val nativePath = nativeArtifact?.path ?: LaunchUtils.libraryToPath("${library.name}:$nativeClassifier")
                var nativeFile = librariesDir.resolve(nativePath)

                if (!nativeFile.exists() && !nativePath.endsWith(".jar")) {
                    nativeFile = librariesDir.resolve("$nativePath.jar")
                }

                if (nativeFile.exists()) {
                    LaunchUtils.extractNatives(nativeFile, nativesDir)
                }
            }
        }
    }

    fun buildPlaceholders(
        baseDir: File,
        installationData: AnyMCInstallationData,
        clientData: Client,
        accountData: ResolvedAccount,
        classpath: String,
        quickPlay: QuickPlay? = null,
        width: Int = 854,
        height: Int = 480
    ): MutableMap<String, String?> {
        val assetsDir = baseDir.resolve("assets")
        val nativesDir = baseDir.resolve("natives")

        val placeholders = mutableMapOf<String, String?>()
        placeholders["version_name"] = installationData.minecraftVersion
        placeholders["game_directory"] = baseDir.resolve("gameFiles").absolutePath
        placeholders["assets_root"] = assetsDir.absolutePath
        placeholders["assets_index_name"] = clientData.assetIndex?.id
        placeholders["auth_player_name"] = accountData.name
        placeholders["auth_uuid"] = accountData.uuid
        placeholders["auth_access_token"] = accountData.token
        placeholders["user_type"] = accountData.type
        placeholders["version_type"] = clientData.type
        placeholders["natives_directory"] = nativesDir.absolutePath
        placeholders["launcher_name"] = "AnyMC"
        placeholders["launcher_version"] = "1.0"
        placeholders["classpath"] = classpath
        
        placeholders["auth_xuid"] = accountData.xuid ?: ""
        placeholders["clientid"] = "anymc"
        placeholders["resolution_width"] = width.toString()
        placeholders["resolution_height"] = height.toString()
        placeholders["user_properties"] = "{}"

        quickPlay?.let {
            placeholders["quickPlayPath"] = it.id
            placeholders["quickPlaySingleplayer"] = if (it.type == QuickPlayType.Singleplayer) it.id else null
            placeholders["quickPlayMultiplayer"] = if (it.type == QuickPlayType.Multiplayer) it.id else null
            placeholders["quickPlayRealms"] = if (it.type == QuickPlayType.Realms) it.id else null
        }

        return placeholders
    }

    fun buildActiveFeatures(isDemo: Boolean, quickPlay: QuickPlay? = null): Set<String> {
        val features = mutableSetOf<String>()
        if (isDemo) features.add("is_demo_user")
        if (quickPlay != null) {
            features.add("has_quick_plays_support")
            when (quickPlay.type) {
                QuickPlayType.Singleplayer -> features.add("is_quick_play_singleplayer")
                QuickPlayType.Multiplayer -> features.add("is_quick_play_multiplayer")
                QuickPlayType.Realms -> features.add("is_quick_play_realms")
            }
        }
        return features
    }

    fun provisionZlibCompat(): String? {
        val os = com.bartixw.anymc.core.utils.PlatformUtils.getCurrentOS()
        if (os != "linux") return null

        val arch = System.getProperty("os.arch")
        val resourceName = when {
            arch.contains("aarch64") -> "/linux-arm64/libz.so.1"
            arch.contains("64") -> "/linux-x64/libz.so.1"
            else -> return null
        }

        val cacheDir = com.bartixw.anymc.core.utils.PlatformUtils.sharedStorageDirectory.resolve("storage").resolve("compat")
        cacheDir.mkdirs()
        val destFile = cacheDir.resolve("libz.so.1")

        if (!destFile.exists()) {
            val stream = this::class.java.getResourceAsStream(resourceName) ?: return null
            stream.use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
        }

        return if (destFile.exists()) destFile.absolutePath else null
    }

    fun getJawtCompatPath(): String? {
        val os = com.bartixw.anymc.core.utils.PlatformUtils.getCurrentOS()
        if (os != "linux") return null

        val storageDir = com.bartixw.anymc.core.utils.PlatformUtils.sharedStorageDirectory.resolve("storage").resolve("compat")
        
        // Use the dedicated shim library. We avoid using libjawt.so from other JREs 
        // because it can lead to symbol conflicts with internal JVM APIs (e.g. JVM_GetClassName).
        val shimFile = storageDir.resolve("libjawt_compat.so")
        return if (shimFile.exists()) shimFile.absolutePath else null
    }

    fun fixExecStack(nativesDir: File) {
        val os = com.bartixw.anymc.core.utils.PlatformUtils.getCurrentOS()
        if (os != "linux") return

        try {
            val natives = nativesDir.listFiles { _, name -> name.endsWith(".so") } ?: return
            natives.forEach { lib ->
                ProcessBuilder("execstack", "-c", lib.absolutePath).start().waitFor()
            }
        } catch (ignored: Exception) {}
    }

    fun patchLwjglNatives(nativesDir: File) {
        val os = com.bartixw.anymc.core.utils.PlatformUtils.getCurrentOS()
        if (os != "linux") return

        val lwjglLib = nativesDir.resolve("liblwjgl64.so").takeIf { it.exists() }
            ?: nativesDir.resolve("liblwjgl.so").takeIf { it.exists() }
            ?: run {
                println("[COMPAT]: LWJGL native not found in ${nativesDir.absolutePath}")
                return
            }

        try {
            val bytes = lwjglLib.readBytes()
            if (bytes.size < 64) return
            
            // ELF Header Check
            if (bytes[0] != 0x7F.toByte() || bytes[1] != 'E'.code.toByte() || bytes[2] != 'L'.code.toByte() || bytes[3] != 'F'.code.toByte()) {
                println("[COMPAT]: ${lwjglLib.name} is not a valid ELF file")
                return
            }
            
            val is64Bit = bytes[4] == 2.toByte()
            val isLittleEndian = bytes[5] == 1.toByte()
            if (!isLittleEndian) return

            fun readU16(offset: Int): Int = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            fun readU32(offset: Int): Long = (readU16(offset).toLong() and 0xFFFF) or ((readU16(offset + 2).toLong() and 0xFFFF) shl 16)
            fun readU64(offset: Int): Long = (readU32(offset) and 0xFFFFFFFFL) or ((readU32(offset + 4) and 0xFFFFFFFFL) shl 32)

            val shOff = if (is64Bit) readU64(40) else readU32(32)
            val shEntSize = readU16(if (is64Bit) 58 else 46)
            val shNum = readU16(if (is64Bit) 60 else 48)

            var dynSymOff = 0L
            var dynSymSize = 0L
            var dynSymEntSize = 0L
            var verSymOff = 0L
            var dynStrOff = 0L
            var dynStrSize = 0L

            for (i in 0 until shNum) {
                val off = (shOff + i * shEntSize).toInt()
                val type = readU32(off + 4)
                when (type) {
                    11L -> { // SHT_DYNSYM
                        dynSymOff = if (is64Bit) readU64(off + 24) else readU32(off + 12)
                        dynSymSize = if (is64Bit) readU64(off + 32) else readU32(off + 16)
                        dynSymEntSize = if (is64Bit) readU64(off + 56) else readU32(off + 36)
                        val strIdx = readU32(off + 12).toInt() // sh_link points to strtab
                        if (strIdx in 0 until shNum) {
                            val strOffHdr = (shOff + strIdx * shEntSize).toInt()
                            dynStrOff = if (is64Bit) readU64(strOffHdr + 24) else readU32(strOffHdr + 12)
                            dynStrSize = if (is64Bit) readU64(strOffHdr + 32) else readU32(strOffHdr + 16)
                        }
                    }
                    0x6fffffffL -> { // SHT_GNU_versym
                        verSymOff = if (is64Bit) readU64(off + 24) else readU32(off + 12)
                    }
                }
            }

            if (dynSymOff == 0L || verSymOff == 0L || dynStrOff == 0L) {
                println("[COMPAT]: Failed to locate ELF sections in ${lwjglLib.name}")
                return
            }

            val symCount = (dynSymSize / dynSymEntSize).toInt()
            for (s in 0 until symCount) {
                val symOff = (dynSymOff + s * dynSymEntSize).toInt()
                val nameIdx = readU32(symOff).toInt()
                
                if (nameIdx in 0 until dynStrSize.toInt()) {
                    val nameBuilder = StringBuilder()
                    var curr = (dynStrOff + nameIdx).toInt()
                    while (curr < bytes.size && bytes[curr] != 0.toByte()) {
                        nameBuilder.append(bytes[curr].toInt().toChar())
                        curr++
                    }
                    
                    if (nameBuilder.toString() == "JAWT_GetAWT") {
                        val vsOff = (verSymOff + s * 2).toInt()
                        bytes[vsOff] = 1
                        bytes[vsOff + 1] = 0
                        lwjglLib.writeBytes(bytes)
                        println("[COMPAT]: Robustly stripped version requirement from JAWT_GetAWT in ${lwjglLib.name}")
                        return
                    }
                }
            }
            println("[COMPAT]: JAWT_GetAWT symbol not found in ${lwjglLib.name}")
        } catch (e: Exception) {
            println("[COMPAT]: Failed to robustly patch LWJGL natives: ${e.message}")
        }
    }

    fun buildFinalCommand(
        baseDir: File,
        installationData: AnyMCInstallationData,
        clientData: Client,
        modLoaderData: Any?,
        placeholders: Map<String, String>,
        activeFeatures: Set<String>,
        installerJarPath: String?,
        maxRamMb: Int,
        currentOS: String,
        isFabric: Boolean,
        isForge: Boolean,
        modulePath: String,
        targetJavaVersion: JavaVersion
    ): List<String> {
        val javaVersionMajor = targetJavaVersion.majorVersion.toLong()
        val javaPath = LaunchUtils.resolveJavaPath(baseDir, targetJavaVersion)

        val command = mutableListOf<String>()
        command.add(javaPath)
        command.add("-Xmx${maxRamMb}M")

        command.addAll(listOf(
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:G1NewSizePercent=20",
            "-XX:G1ReservePercent=20",
            "-XX:MaxGCPauseMillis=50",
            "-XX:G1HeapRegionSize=32M",
            "-Danymc.mainclass=net.minecraft.client.main.Main"
        ))

        val jvmArgs = clientData.arguments?.jvm ?: emptyList()
        jvmArgs.forEach { element ->
            when (element) {
                is JVMElement.StringValue -> {
                    val resolved = LaunchUtils.resolvePlaceholder(element.value, placeholders)
                    if (LaunchUtils.shouldIncludeJvmArg(resolved, javaVersionMajor)) command.add(resolved)
                }
                is JVMElement.JVMClassValue -> {
                    if (LaunchUtils.isJvmRuleAllowed(element.rules, currentOS)) {
                        when (val v = element.value) {
                            is Value.StringValue -> {
                                val resolved = LaunchUtils.resolvePlaceholder(v.value, placeholders)
                                if (LaunchUtils.shouldIncludeJvmArg(resolved, javaVersionMajor)) command.add(resolved)
                            }
                            is Value.StringArrayValue -> v.values.forEach {
                                val resolved = LaunchUtils.resolvePlaceholder(it, placeholders)
                                if (LaunchUtils.shouldIncludeJvmArg(resolved, javaVersionMajor)) command.add(resolved)
                            }
                        }
                    }
                }
            }
        }

        if (command.none { it.startsWith("-Djava.library.path") }) {
            command.add("-Djava.library.path=${baseDir.resolve("natives").absolutePath}")
        }

        if (isFabric) {
            command.add("-Dfabric.gameJarPath=${baseDir.resolve("${installationData.minecraftVersion}.jar").absolutePath}")
        }

        val isModularNeoForge = isForge && installationData.minecraftVersion.startsWith("1.21")
        val finalMainClass = when {
            isModularNeoForge -> "cpw.mods.bootstraplauncher.BootstrapLauncher"
            modLoaderData is FabricProfile -> modLoaderData.mainClass
            modLoaderData is MultiMCProfile -> modLoaderData.mainClass ?: clientData.mainClass
            else -> clientData.mainClass
        } ?: throw Exception("Main class not found")

        if (isModularNeoForge) {
            if (modulePath.isNotEmpty()) {
                command.add("-p")
                command.add(modulePath)
                command.add("--add-modules")
                command.add("ALL-MODULE-PATH")
            }

            command.add("--add-opens")
            command.add("java.base/java.util.jar=cpw.mods.securejarhandler")
            command.add("--add-opens")
            command.add("java.base/java.lang.invoke=cpw.mods.securejarhandler")
            command.add("--add-exports")
            command.add("java.base/sun.security.util=cpw.mods.securejarhandler")
            command.add("--add-exports")
            command.add("jdk.naming.dns/com.sun.jndi.dns=java.naming")

            command.addAll(listOf(
                "--add-opens", "java.base/java.lang.invoke=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                "--add-opens", "java.base/java.util=ALL-UNNAMED"
            ))

            command.add("-DlibraryDirectory=${baseDir.resolve("libraries").absolutePath}")
            command.add("-Dmc.mainClass=net.minecraft.client.main.Main")
            command.add("-Dfml.mavenRoots=${baseDir.resolve("libraries").absolutePath}")

            listOf("ALL-UNNAMED", "cpw.mods.securejarhandler", "fmlloader", "fmlearlydisplay").forEach { module ->
                command.add("--add-reads")
                command.add("$module=minecraft")
            }
            command.add("--add-reads")
            command.add("minecraft=ALL-UNNAMED")
            command.add("-Dneoforge.earlyWindowControl=false")
        } else if (modLoaderData is MultiMCProfile && finalMainClass == "io.github.zekerzhayard.forgewrapper.installer.Main") {
            command.add("-Dforgewrapper.librariesDir=${baseDir.resolve("libraries").absolutePath}")
            command.add("-Dforgewrapper.minecraft=${baseDir.resolve("${installationData.minecraftVersion}.jar").absolutePath}")
            command.add("-Dforgewrapper.skipHashCheck=true")
            installerJarPath?.let { command.add("-Dforgewrapper.installer=$it") }

            if (installationData.loaderType == "neoforge") {
                command.add("-Dneoforge.earlyWindowProvider=none")
                command.add("-Dforge.earlyWindowProvider=none")
                command.add("-Dfml.earlyWindowProvider=none")
                command.add("-Dneoforge.earlyWindowControl=false")
                command.add("-Dforge.earlyWindowControl=false")
                command.add("-Dfml.earlyWindowControl=false")
                command.add("-Dfml.earlyprogresswindow=false")
            }

            val isModularForge = (modLoaderData.libraries + modLoaderData.mavenFiles).any { it.name.contains("bootstraplauncher") }
            if (isModularForge) {
                command.add("--add-exports")
                command.add("java.base/sun.security.util=ALL-UNNAMED")
                command.add("--add-opens")
                command.add("java.base/java.util.jar=ALL-UNNAMED")
                command.add("--add-exports")
                command.add("jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED")

                val coreModules = listOf(
                    "fml_earlydisplay", "fml_loader", "cpw.mods.modlauncher", "cpw.mods.securejarhandler",
                    "fmlloader", "fmlearlydisplay"
                )
                coreModules.forEach { module ->
                    command.add("--add-reads")
                    command.add("$module=ALL-UNNAMED")
                }
            }
        }

        if (modLoaderData is MultiMCProfile) {
            modLoaderData.extraJvmArgs?.forEach { command.add(LaunchUtils.resolvePlaceholder(it, placeholders)) }
            modLoaderData.systemProperties?.forEach { (k, v) ->
                command.add("-D$k=${LaunchUtils.resolvePlaceholder(v, placeholders)}")
            }
        }

        command.addAll(installationData.jvmArgs)

        if (command.none { it == "-cp" || it == "-classpath" }) {
            command.add("-cp")
            command.add(placeholders["classpath"] ?: "")
        }

        command.add(finalMainClass)

        val rawGameArgs = mutableListOf<String>()
        val gameArgs = clientData.arguments?.game ?: emptyList()

        if (gameArgs.isNotEmpty()) {
            gameArgs.forEach { element ->
                when (element) {
                    is GameElement.StringValue -> rawGameArgs.add(LaunchUtils.resolvePlaceholder(element.value, placeholders))
                    is GameElement.GameClassValue -> {
                        if (LaunchUtils.isGameRuleAllowed(element.rules, activeFeatures)) {
                            when (val v = element.value) {
                                is Value.StringValue -> rawGameArgs.add(LaunchUtils.resolvePlaceholder(v.value, placeholders))
                                is Value.StringArrayValue -> v.values.forEach {
                                    val resolved = LaunchUtils.resolvePlaceholder(it, placeholders)
                                    if (!resolved.contains("${'$'}{")) rawGameArgs.add(resolved)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            clientData.minecraftArguments?.split(" ")?.filter { it.isNotEmpty() }?.forEach {
                rawGameArgs.add(LaunchUtils.resolvePlaceholder(it, placeholders))
            }
        }

        if (modLoaderData is MultiMCProfile) {
            modLoaderData.minecraftArguments?.split(" ")?.filter { it.isNotEmpty() }?.forEach { rawGameArgs.add(LaunchUtils.resolvePlaceholder(it, placeholders)) }
            modLoaderData.extraMinecraftArguments?.split(" ")?.filter { it.isNotEmpty() }?.forEach { rawGameArgs.add(LaunchUtils.resolvePlaceholder(it, placeholders)) }
        }

        val finalGameArgs = mutableListOf<String>()
        val keyToValue = mutableMapOf<String, String>()
        val tweakClasses = (modLoaderData as? MultiMCProfile)?.extraTweakers?.toMutableList() ?: mutableListOf()

        var argIter = 0
        while (argIter < rawGameArgs.size) {
            val arg = rawGameArgs[argIter]
            if (arg == "--tweakClass" && argIter + 1 < rawGameArgs.size) {
                val tweaker = rawGameArgs[argIter + 1]
                if (!isFabric || tweaker != "net.minecraft.launchwrapper.VanillaTweaker") {
                    if (tweaker !in tweakClasses) tweakClasses.add(tweaker)
                }
                argIter += 2
            } else if (arg.startsWith("--") && argIter + 1 < rawGameArgs.size && !rawGameArgs[argIter + 1].startsWith("--")) {
                keyToValue[arg] = rawGameArgs[argIter + 1]
                argIter += 2
            } else {
                if (arg !in finalGameArgs) finalGameArgs.add(arg)
                argIter++
            }
        }

        val processGameArgs = mutableListOf<String>()
        keyToValue.forEach { (k, v) -> processGameArgs.addAll(listOf(k, v)) }
        tweakClasses.forEach { processGameArgs.addAll(listOf("--tweakClass", it)) }
        processGameArgs.addAll(finalGameArgs)

        if (installationData.loaderType == "neoforge") {
            processGameArgs.addAll(listOf("--fml.earlyWindowControl", "false"))
        }

        command.addAll(processGameArgs)

        val filteredCommand = mutableListOf<String>()
        var argIdx = 0
        while (argIdx < command.size) {
            val arg = command[argIdx]
            if (installationData.removedArgs.contains(arg)) {
                argIdx += if (argIdx + 1 < command.size && !command[argIdx + 1].startsWith("-")) 2 else 1
                continue
            }
            filteredCommand.add(arg)
            argIdx++
        }

        return filteredCommand
    }
}
