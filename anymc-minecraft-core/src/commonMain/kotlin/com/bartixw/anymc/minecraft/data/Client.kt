/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.minecraft.data

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.builtins.*

@Serializable
data class Client (
    val arguments: Arguments? = null,
    val minecraftArguments: String? = null,
    val assetIndex: AssetIndex? = null,
    val assets: String? = null,
    val complianceLevel: Int? = null,
    val downloads: Downloads? = null,
    val id: String? = null,
    val javaVersion: JavaVersion? = null,
    val libraries: List<Library> = emptyList(),
    val logging: Logging? = null,
    val mainClass: String? = null,
    val minimumLauncherVersion: Int? = null,
    val releaseTime: String? = null,
    val time: String? = null,
    val type: String? = null,
    val inheritsFrom: String? = null,
    val mavenFiles: List<Library>? = null
) {
    val effectiveJavaVersion: JavaVersion
        get() = javaVersion ?: JavaVersion.DEFAULT
}

@Serializable
data class Arguments (
    @SerialName("default-user-jvm")
    val defaultUserJVM: List<DefaultUserJVM>? = null,

    val game: List<GameElement>? = null,
    val jvm: List<JVMElement>? = null
)

@Serializable
data class DefaultUserJVM (
    val value: List<String>,
    val rules: List<DefaultUserJVMRule>? = null
)

@Serializable
data class DefaultUserJVMRule (
    val action: Action,
    val os: OS
)

@Serializable
enum class Action(val value: String) {
    @SerialName("allow") Allow("allow"),
    @SerialName("disallow") Disallow("disallow");
}

@Serializable
data class OS (
    val name: OSName? = null,
    val arch: String? = null,
    val versionRange: VersionRange? = null
)

@Serializable
enum class OSName(val value: String) {
    @SerialName("linux") Linux("linux"),
    @SerialName("osx") Osx("osx"),
    @SerialName("windows") Windows("windows");
}

@Serializable
data class VersionRange (
    val min: String? = null,
    val max: String? = null
)

@Serializable(with = GameElementSerializer::class)
sealed class GameElement {
    @Serializable
    data class GameClassValue(
        val rules: List<GameRule>,
        val value: Value
    ) : GameElement()

    @Serializable
    data class StringValue(val value: String) : GameElement()
}

object GameElementSerializer : KSerializer<GameElement> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("GameElement")

    override fun deserialize(decoder: Decoder): GameElement {
        val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be decoded only by Json")
        val element = input.decodeJsonElement()
        return if (element is JsonPrimitive) {
            GameElement.StringValue(element.content)
        } else {
            input.json.decodeFromJsonElement(GameElement.GameClassValue.serializer(), element)
        }
    }

    override fun serialize(encoder: Encoder, value: GameElement) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be encoded only by Json")
        when (value) {
            is GameElement.StringValue -> output.encodeJsonElement(JsonPrimitive(value.value))
            is GameElement.GameClassValue -> output.encodeJsonElement(output.json.encodeToJsonElement(GameElement.GameClassValue.serializer(), value))
        }
    }
}


@Serializable
data class GameRule (
    val action: Action,
    val features: Features? = null
)

@Serializable
data class Features (
    @SerialName("is_demo_user")
    val isDemoUser: Boolean? = null,

    @SerialName("has_custom_resolution")
    val hasCustomResolution: Boolean? = null,

    @SerialName("has_quick_plays_support")
    val hasQuickPlaysSupport: Boolean? = null,

    @SerialName("is_quick_play_singleplayer")
    val isQuickPlaySingleplayer: Boolean? = null,

    @SerialName("is_quick_play_multiplayer")
    val isQuickPlayMultiplayer: Boolean? = null,

    @SerialName("is_quick_play_realms")
    val isQuickPlayRealms: Boolean? = null
)

@Serializable(with = ValueSerializer::class)
sealed class Value {
    @Serializable
    data class StringArrayValue(val values: List<String>) : Value()
    @Serializable
    data class StringValue(val value: String)            : Value()
}

object ValueSerializer : KSerializer<Value> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Value")

    override fun deserialize(decoder: Decoder): Value {
        val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be decoded only by Json")
        val element = input.decodeJsonElement()
        return if (element is JsonArray) {
            Value.StringArrayValue(input.json.decodeFromJsonElement(ListSerializer(String.serializer()), element))
        } else {
            Value.StringValue(element.jsonPrimitive.content)
        }
    }

    override fun serialize(encoder: Encoder, value: Value) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be encoded only by Json")
        when (value) {
            is Value.StringValue -> output.encodeJsonElement(JsonPrimitive(value.value))
            is Value.StringArrayValue -> output.encodeJsonElement(output.json.encodeToJsonElement(ListSerializer(String.serializer()), value.values))
        }
    }
}

@Serializable(with = JVMElementSerializer::class)
sealed class JVMElement {
    @Serializable
    data class JVMClassValue(
        val rules: List<JVMRule>,
        val value: Value
    ) : JVMElement()

    @Serializable
    data class StringValue(val value: String)     : JVMElement()
}

object JVMElementSerializer : KSerializer<JVMElement> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("JVMElement")

    override fun deserialize(decoder: Decoder): JVMElement {
        val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be decoded only by Json")
        val element = input.decodeJsonElement()
        return if (element is JsonPrimitive) {
            JVMElement.StringValue(element.content)
        } else {
            input.json.decodeFromJsonElement(JVMElement.JVMClassValue.serializer(), element)
        }
    }

    override fun serialize(encoder: Encoder, value: JVMElement) {
        val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be encoded only by Json")
        when (value) {
            is JVMElement.StringValue -> output.encodeJsonElement(JsonPrimitive(value.value))
            is JVMElement.JVMClassValue -> output.encodeJsonElement(output.json.encodeToJsonElement(JVMElement.JVMClassValue.serializer(), value))
        }
    }
}


@Serializable
data class JVMRule (
    val action: Action,
    val os: OS? = null
)

@Serializable
data class AssetIndex (
    val id: String,
    val sha1: String,
    val size: Long,
    val totalSize: Long? = null,
    val url: String
)

@Serializable
data class Downloads (
    val client: ServerClass? = null,
    val server: ServerClass? = null
)

@Serializable
data class ServerClass (
    val sha1: String,
    val size: Long,
    val url: String,
    val path: String? = null
)

@Serializable
data class JavaVersion (
    val component: String,
    val majorVersion: Int
) {
    companion object {
        val DEFAULT = JavaVersion("java-runtime-alpha", 8)
    }
}

@Serializable
data class JavaRuntimeManifest(
    val files: Map<String, JavaRuntimeFile>
)

@Serializable
data class JavaRuntimeFile(
    val type: String,
    val downloads: JavaRuntimeDownloads? = null,
    val executable: Boolean? = null,
    val target: String? = null
)

@Serializable
data class JavaRuntimeDownloads(
    val lzma: JavaRuntimeDownload? = null,
    val raw: JavaRuntimeDownload
)

@Serializable
data class JavaRuntimeDownload(
    val sha1: String,
    val size: Long,
    val url: String
)

@Serializable
data class Library (
    val downloads: LibraryDownloads? = null,
    val name: String,
    val rules: List<LibraryRule>? = null,
    val natives: Map<String, String>? = null,
    val extract: Extract? = null,
    val url: String? = null
) {
    fun isAllowed(osName: String): Boolean {
        if (rules.isNullOrEmpty()) return true

        var allowed = false

        for (rule in rules) {
            val osMatch = rule.os == null || rule.os.name.value.equals(osName, ignoreCase = true)

            if (osMatch) {
                allowed = rule.action == Action.Allow
            }
        }

        return allowed
    }
}

@Serializable
data class Extract(
    val exclude: List<String>? = null
)

@Serializable
data class LibraryDownloads (
    val artifact: ServerClass? = null,
    val classifiers: Map<String, Artifact>? = null
)

@Serializable
data class Artifact(
    val path: String,
    val sha1: String,
    val size: Long,
    val url: String
)

@Serializable
data class LibraryRule (
    val action: Action,
    val os: OSNameOnly? = null
)

@Serializable
data class OSNameOnly (
    val name: OSName
)

@Serializable
data class AssetIndexContent(
    val objects: Map<String, AssetObject>
)

@Serializable
data class AssetObject(
    val hash: String,
    val size: Long
)

@Serializable
data class Logging (
    val client: LoggingClient
)

@Serializable
data class LoggingClient (
    val argument: String,
    val file: AssetIndex,
    val type: String
)