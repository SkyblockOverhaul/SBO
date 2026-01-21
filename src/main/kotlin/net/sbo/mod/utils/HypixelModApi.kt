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
    var isOnSkyblock: Boolean = false
    var isLeader: Boolean = false
    var isInParty: Boolean = false
    var partyMembers: List<String> = emptyList()
    var mode: String = ""

    // listeners
    private val partyInfoListeners = mutableListOf<(isInParty: Boolean, isLeader: Boolean, members: List<String>) -> Unit>()
    private val errorListeners = mutableListOf<(packet: ErrorS2CPacket) -> Unit>()

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
        mode = ""
    }

    private fun handlePacket(packet: HypixelS2CPacket) {
        when (packet) {
            is HelloS2CPacket -> onHelloPacket(packet)
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

    private fun onHelloPacket(packet: HelloS2CPacket) {
        isOnHypixel = true
        sendPartyInfoPacket()
    }

    private fun onPartyInfoPacket(packet: PartyInfoS2CPacket) {
        this.isInParty = packet.inParty

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
        this.partyMembers = membersList

        partyInfoListeners.forEach { listener ->
            listener(this.isInParty, this.isLeader, this.partyMembers)
        }
    }

    fun onPartyInfo(listener: (isInParty: Boolean, isLeader: Boolean, members: List<String>) -> Unit) {
        partyInfoListeners.add(listener)
    }

    private fun onErrorPacket(packet: ErrorS2CPacket) {
        if (packet.id == LocationUpdateS2CPacket.ID) {
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