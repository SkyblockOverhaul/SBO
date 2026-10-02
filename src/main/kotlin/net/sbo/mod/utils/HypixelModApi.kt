package net.sbo.mod.utils

import net.azureaaron.hmapi.events.HypixelPacketEvents
import net.azureaaron.hmapi.network.HypixelNetworking
import net.azureaaron.hmapi.network.packet.s2c.ErrorS2CPacket
import net.azureaaron.hmapi.network.packet.s2c.HelloS2CPacket
import net.azureaaron.hmapi.network.packet.s2c.HypixelS2CPacket
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket
import net.azureaaron.hmapi.network.packet.v2.s2c.PartyInfoS2CPacket
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.DisconnectEvent

object HypixelModApi {
    var isOnHypixel: Boolean = false
    private var isOnSkyblock: Boolean = false
    private var isLeader: Boolean = false
    private var isInParty: Boolean = false
    private var partyMembers: List<String> = emptyList()
    // Party role per member uuid without dashes: LEADER, MODERATOR or MEMBER
    private var partyRoles: Map<String, String> = emptyMap()
    private var mode: String = ""

    // listeners
    private val partyInfoListeners = mutableListOf<(isInParty: Boolean, isLeader: Boolean, members: List<String>) -> Unit>()
    private val errorListeners = mutableListOf<(packet: ErrorS2CPacket) -> Unit>()

    // Dev party finder simulation answers party info requests instead of Hypixel, null otherwise
    internal var partyInfoOverride: (() -> Unit)? = null

    fun init() {
        HypixelPacketEvents.HELLO.register(::handlePacket)
        HypixelPacketEvents.PARTY_INFO.register(::handlePacket)
        HypixelPacketEvents.LOCATION_UPDATE.register(::handlePacket)
    }

    @SboEvent
    fun onDisconnect(event: DisconnectEvent) {
        isOnHypixel = false
        isOnSkyblock = false
        isLeader = false
        isInParty = false
        partyMembers = emptyList()
        partyRoles = emptyMap()
        mode = ""
    }

    private fun handlePacket(packet: HypixelS2CPacket) {
        when (packet) {
            is HelloS2CPacket -> onHelloPacket()
            is LocationUpdateS2CPacket -> onLocationUpdatePacket(packet)
            is PartyInfoS2CPacket -> onPartyInfoPacket(packet)
            is ErrorS2CPacket -> onErrorPacket(packet)
            else -> {}
        }
    }

    private fun onLocationUpdatePacket(packet: LocationUpdateS2CPacket) {
        isOnSkyblock = packet.serverType.orElse("") == "SKYBLOCK"
        mode = packet.mode.orElse("")
    }

    private fun onHelloPacket() {
        isOnHypixel = true
        sendPartyInfoPacket()
    }

    private fun onPartyInfoPacket(packet: PartyInfoS2CPacket) {
        this.isInParty = packet.inParty
        partyRoles = packet.members?.entries?.associate { (uuid, role) -> uuid.toString().replace("-", "") to role.toString() } ?: emptyMap()

        val membersList = packet.members?.map { it.key.toString() }?.toMutableList() ?: mutableListOf()
        if (isInParty) {
            val leaderUUID = packet.members?.entries?.find { it.value.toString() == "LEADER" }?.key.toString()

            membersList.remove(leaderUUID)
            membersList.add(0, leaderUUID)

            this.isLeader = packet.members?.get(Player.getUUID())?.toString() == "LEADER"
        } else {
            this.isLeader = true
            membersList.add(Player.getUUIDString())
        }
        deliverPartyInfo(isInParty, isLeader, membersList)
    }

    /** Passes party info to the listeners; [members] start with the leader. */
    internal fun deliverPartyInfo(isInParty: Boolean, isLeader: Boolean, members: List<String>) {
        this.isInParty = isInParty
        this.isLeader = isLeader
        this.partyMembers = members

        partyInfoListeners.forEach { listener ->
            listener(this.isInParty, this.isLeader, this.partyMembers)
        }
    }

    /** LEADER, MODERATOR or MEMBER from the last party packet, null when unknown. */
    fun partyRole(uuid: String): String? = partyRoles[uuid.replace("-", "")]

    /** After a promote, until the next party packet confirms it. */
    fun markModerator(uuid: String) {
        partyRoles = partyRoles + (uuid.replace("-", "") to "MODERATOR")
    }

    fun onPartyInfo(listener: (isInParty: Boolean, isLeader: Boolean, members: List<String>) -> Unit) {
        partyInfoListeners.add(listener)
    }

    private fun onErrorPacket(packet: ErrorS2CPacket) {
        if (packet.type() == LocationUpdateS2CPacket.ID) {
            isOnSkyblock = false
            mode = ""
        }

        errorListeners.forEach { listener ->
            listener(packet)
        }
    }

    fun onError(listener: (packet: ErrorS2CPacket) -> Unit) {
        errorListeners.add(listener)
    }

    fun sendPartyInfoPacket(createParty: Boolean = false) {
        partyInfoOverride?.let { answer ->
            if (createParty) PartyFinderManager.creatingParty = true
            answer()
            return
        }
        try {
            if (isOnHypixel) {
                if (createParty) PartyFinderManager.creatingParty = true
                HypixelNetworking.sendPartyInfoC2SPacket(2)
            } else {
                PartyFinderManager.creatingParty = false
                Chat.chat("§6[SBO] §eYou are not on Hypixel. You can only use this feature on Hypixel.")
            }
        } catch (_: Exception) {
            PartyFinderManager.creatingParty = false
        }
    }
}