/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc.core.tailscale

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Instant

typealias NetipAddr = String
typealias NetipPrefix = String
typealias KeyNodePublic = String
typealias KeyNLPublic = String
typealias TailcfgNodeID = Long
typealias TailcfgStableNodeID = String
typealias TailcfgStableTailnetID = String
typealias TailcfgUserID = Long
typealias TailcfgUserProfile = Map<String, String>
typealias TailcfgDNSRecord = Map<String, String>
typealias TailcfgClientVersion = Map<String, String>
typealias TailcfgNodeCapMap = Map<String, List<String>>
typealias TailcfgLocation = Map<String, String>
typealias TkaNodeKeySignature = String
typealias NodecapCap = String

object GoTimeSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("GoInstantSerializer", PrimitiveKind.STRING)

    private const val GO_ZERO_TIME = "0001-01-01T00:00:00Z"

    override fun deserialize(decoder: Decoder): Instant {
        val stringValue = decoder.decodeString()
        if (stringValue == GO_ZERO_TIME || stringValue.isEmpty()) {
            return Instant.DISTANT_PAST
        }
        return Instant.parse(stringValue)
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        if (value == Instant.DISTANT_PAST) {
            encoder.encodeString(GO_ZERO_TIME)
        } else {
            encoder.encodeString(value.toString())
        }
    }
}

/**
 * Status represents the entire state of the IPN network.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Status(
    /** Version is the daemon's long version (see version.Long). */
    @SerialName("Version")
    val version: String,

    /**
     * TUN is whether /dev/net/tun (or equivalent kernel interface) is being
     * used. If false, it's running in userspace mode.
     */
    @SerialName("TUN")
    val tun: Boolean,

    /**
     * BackendState is an ipn.State string value:
     * "NoState", "NeedsLogin", "NeedsMachineAuth", "Stopped", "Starting", "Running".
     */
    @SerialName("BackendState")
    val backendState: String,

    /** HaveNodeKey is whether the current profile has a node key configured. */
    @SerialName("HaveNodeKey")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val haveNodeKey: Boolean? = null,

    /** Current URL provided by control to authorize client. */
    @SerialName("AuthURL")
    val authURL: String,

    /** Tailscale IP(s) assigned to this node. */
    @SerialName("TailscaleIPs")
    val tailscaleIPs: List<NetipAddr> = emptyList(),

    @SerialName("Self")
    val self: PeerStatus? = null,

    /**
     * ExitNodeStatus describes the current exit node.
     * If null, an exit node is not in use.
     */
    @SerialName("ExitNodeStatus")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val exitNodeStatus: ExitNodeStatus? = null,

    /**
     * Health contains health check problems.
     * Empty means everything is good.
     */
    @SerialName("Health")
    val health: List<String> = emptyList(),

    /**
     * This field is the legacy name of CurrentTailnet.MagicDNSSuffix.
     * @deprecated Use [CurrentTailnet.magicDNSSuffix] instead.
     */
    @Deprecated("Use currentTailnet?.magicDNSSuffix instead")
    @SerialName("MagicDNSSuffix")
    val magicDNSSuffix: String,

    /**
     * CurrentTailnet is information about the tailnet that the node
     * is currently connected to. When not connected, this field is null.
     */
    @SerialName("CurrentTailnet")
    val currentTailnet: TailnetStatus? = null,

    /**
     * CertDomains are the set of DNS names for which the control
     * plane server will assist with provisioning TLS certificates.
     */
    @SerialName("CertDomains")
    val certDomains: List<String> = emptyList(),

    /** ExtraRecords contains extra DNS records to add to the DNS resolver. */
    @SerialName("ExtraRecords")
    val extraRecords: List<TailcfgDNSRecord> = emptyList(),

    /** Peer is the state of each peer, keyed by each peer's current public key. */
    @SerialName("Peer")
    val peer: Map<KeyNodePublic, PeerStatus> = emptyMap(),

    /** Profile information about UserIDs referenced by PeerStatus.UserID, etc. */
    @SerialName("User")
    val user: Map<TailcfgUserID, TailcfgUserProfile> = emptyMap(),

    /** ClientVersion contains information about the latest available Tailscale client version. */
    @SerialName("ClientVersion")
    val clientVersion: TailcfgClientVersion? = null
) {
    /**
     * Odpowiednik metody `Peers()` w Go: zwraca posortowaną listę kluczy węzłów (peers).
     */
    val peers: List<KeyNodePublic>
        get() = peer.keys.sorted()
}

/**
 * TKAKey describes a key trusted by tailnet lock.
 */
@Serializable
data class TKAKey(
    @SerialName("Kind")
    val kind: String,

    @SerialName("Key")
    val key: KeyNLPublic,

    @SerialName("Metadata")
    val metadata: Map<String, String> = emptyMap(),

    @SerialName("Votes")
    val votes: ULong
)

/**
 * TKAPeer describes a peer and its tailnet lock details.
 */
@Serializable
data class TKAPeer(
    @SerialName("Name")
    val name: String, // DNS

    @SerialName("ID")
    val id: TailcfgNodeID,

    @SerialName("StableID")
    val stableID: TailcfgStableNodeID,

    @SerialName("TailscaleIPs")
    val tailscaleIPs: List<NetipAddr> = emptyList(),

    @SerialName("NodeKey")
    val nodeKey: KeyNodePublic,

    @SerialName("NodeKeySignature")
    val nodeKeySignature: TkaNodeKeySignature
)

/**
 * TailnetLockStatus represents whether tailnet-lock is enabled,
 * along with details about the locally-known state of the tailnet key authority.
 */
@Serializable
data class TailnetLockStatus(
    /** Enabled is true if tailnet lock is enabled. */
    @SerialName("Enabled")
    val enabled: Boolean,

    /** Head describes the AUM hash of the leaf AUM. Head is null if tailnet lock is not enabled. */
    @SerialName("Head")
    val head: ByteArray? = null,

    /** PublicKey describes the node's tailnet-lock public key. */
    @SerialName("PublicKey")
    val publicKey: KeyNLPublic,

    /** NodeKey describes the node's current node-key. */
    @SerialName("NodeKey")
    val nodeKey: KeyNodePublic? = null,

    /** NodeKeySigned is true if our node is authorized by tailnet-lock. */
    @SerialName("NodeKeySigned")
    val nodeKeySigned: Boolean,

    /** NodeKeySignature is the current signature of this node's key. */
    @SerialName("NodeKeySignature")
    val nodeKeySignature: TkaNodeKeySignature? = null,

    /** TrustedKeys describes the keys currently trusted to make changes to tailnet-lock. */
    @SerialName("TrustedKeys")
    val trustedKeys: List<TKAKey> = emptyList(),

    /** VisiblePeers describes peers which are visible in the netmap with valid signatures. */
    @SerialName("VisiblePeers")
    val visiblePeers: List<TKAPeer> = emptyList(),

    /** FilteredPeers describes peers removed due to failing tailnet lock checks. */
    @SerialName("FilteredPeers")
    val filteredPeers: List<TKAPeer> = emptyList(),

    /** StateID is a nonce associated with the tailnet lock authority. */
    @SerialName("StateID")
    val stateID: ULong
)

/** Deprecated: use [TailnetLockStatus] instead. */
@Deprecated("Use TailnetLockStatus instead", ReplaceWith("TailnetLockStatus"))
typealias NetworkLockStatus = TailnetLockStatus

/**
 * TailnetLockUpdate describes a change to tailnet-lock state.
 */
@Serializable
data class TailnetLockUpdate(
    @SerialName("Hash")
    val hash: ByteArray,

    @SerialName("Change")
    val change: String,

    @SerialName("Raw")
    val raw: ByteArray
)

/** Deprecated: use [TailnetLockUpdate] instead. */
@Deprecated("Use TailnetLockUpdate instead", ReplaceWith("TailnetLockUpdate"))
typealias NetworkLockUpdate = TailnetLockUpdate

/**
 * TailnetStatus is information about a Tailscale network ("tailnet").
 */
@Serializable
data class TailnetStatus(
    /** Name is the name of the network that's currently in use. */
    @SerialName("Name")
    val name: String,

    /** StableID is the stable, unique identifier of the tailnet. */
    @SerialName("StableID")
    val stableID: TailcfgStableTailnetID,

    /** MagicDNSSuffix is the network's MagicDNS suffix (e.g. "userfoo.tailscale.net"). */
    @SerialName("MagicDNSSuffix")
    val magicDNSSuffix: String,

    /** MagicDNSEnabled is whether the network has MagicDNS enabled. */
    @SerialName("MagicDNSEnabled")
    val magicDNSEnabled: Boolean
)

/**
 * ExitNodeStatus describes the current exit node.
 */
@Serializable
data class ExitNodeStatus(
    /** ID is the exit node's ID. */
    @SerialName("ID")
    val id: TailcfgStableNodeID,

    /** Online is whether the exit node is alive. */
    @SerialName("Online")
    val online: Boolean,

    /** TailscaleIPs are the exit node's IP addresses assigned to the node. */
    @SerialName("TailscaleIPs")
    val tailscaleIPs: List<NetipPrefix> = emptyList()
)

@Serializable
data class PeerStatusLite(
    /** NodeKey is this peer's public node key. */
    @SerialName("NodeKey")
    val nodeKey: KeyNodePublic,

    /** TxBytes/RxBytes are total bytes transmitted/received. */
    @SerialName("TxBytes")
    val txBytes: Long,

    @SerialName("RxBytes")
    val rxBytes: Long,

    /** LastHandshake is the last time a handshake succeeded with this peer. */
    @SerialName("LastHandshake")
    @Serializable(with = GoTimeSerializer::class)
    val lastHandshake: Instant
)

/**
 * PeerStatus describes a peer node and its current state.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PeerStatus(
    @SerialName("ID")
    val id: TailcfgStableNodeID,

    @SerialName("NodeID")
    val nodeID: TailcfgNodeID,

    @SerialName("PublicKey")
    val publicKey: KeyNodePublic,

    @SerialName("HostName")
    val hostName: String,

    /** DNSName is the Peer's FQDN (ends with a dot). */
    @SerialName("DNSName")
    val dnsName: String,

    @SerialName("OS")
    val os: String,

    @SerialName("UserID")
    val userID: TailcfgUserID,

    /** AltSharerUserID is the user who shared this node if different than UserID. */
    @SerialName("AltSharerUserID")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val altSharerUserID: TailcfgUserID? = null,

    /** TailscaleIPs are the IP addresses assigned to the node. */
    @SerialName("TailscaleIPs")
    val tailscaleIPs: List<NetipAddr> = emptyList(),

    /** AllowedIPs are IP addresses allowed to route to this node. */
    @SerialName("AllowedIPs")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val allowedIPs: List<NetipPrefix>? = null,

    /** Tags are the list of ACL tags applied to this node. */
    @SerialName("Tags")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tags: List<String>? = null,

    /** PrimaryRoutes are the routes this node is currently the primary subnet router for. */
    @SerialName("PrimaryRoutes")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val primaryRoutes: List<NetipPrefix>? = null,

    @SerialName("Addrs")
    val addrs: List<String> = emptyList(),

    @SerialName("CurAddr")
    val curAddr: String,

    @SerialName("Relay")
    val relay: String,

    @SerialName("PeerRelay")
    val peerRelay: String,

    @SerialName("RxBytes")
    val rxBytes: Long,

    @SerialName("TxBytes")
    val txBytes: Long,

    @SerialName("Created")
    @Serializable(with = GoTimeSerializer::class)
    val created: Instant,

    @SerialName("LastWrite")
    @Serializable(with = GoTimeSerializer::class)
    val lastWrite: Instant,

    @SerialName("LastSeen")
    @Serializable(with = GoTimeSerializer::class)
    val lastSeen: Instant,

    @SerialName("LastHandshake")
    @Serializable(with = GoTimeSerializer::class)
    val lastHandshake: Instant,

    @SerialName("Online")
    val online: Boolean,

    @SerialName("ExitNode")
    val exitNode: Boolean,

    @SerialName("ExitNodeOption")
    val exitNodeOption: Boolean,

    @SerialName("Active")
    val active: Boolean,

    @SerialName("PeerAPIURL")
    val peerAPIURL: List<String> = emptyList(),

    @SerialName("TaildropTarget")
    val taildropTarget: TaildropTargetStatus,

    @SerialName("NoFileSharingReason")
    val noFileSharingReason: String,

    /**
     * Capabilities that the node has.
     * @deprecated Use [capMap] instead.
     */
    @Deprecated("Use capMap instead")
    @SerialName("Capabilities")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val capabilities: List<NodecapCap>? = null,

    @SerialName("CapMap")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val capMap: TailcfgNodeCapMap? = null,

    @SerialName("sshHostKeys")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val sshHostKeys: List<String>? = null,

    @SerialName("ShareeNode")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val shareeNode: Boolean? = null,

    @SerialName("InNetworkMap")
    val inNetworkMap: Boolean,

    @SerialName("InMagicSock")
    val inMagicSock: Boolean,

    @SerialName("InEngine")
    val inEngine: Boolean,

    @SerialName("Expired")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val expired: Boolean? = null,

    @SerialName("KeyExpiry")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @Serializable(with = GoTimeSerializer::class)
    val keyExpiry: Instant? = null,

    @SerialName("Location")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val location: TailcfgLocation? = null
)

/**
 * Representation of TaildropTargetStatus in Kotlin.
 * In Go, it's defined as `type TaildropTargetStatus int`.
 */
@Serializable
@JvmInline
value class TaildropTargetStatus(val value: Int)