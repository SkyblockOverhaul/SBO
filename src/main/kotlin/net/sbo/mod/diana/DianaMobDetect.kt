package net.sbo.mod.diana

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ProfileComponent
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.decoration.ArmorStandEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.sbo.mod.utils.events.Register
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.getSecondsPassed
import net.sbo.mod.utils.Helper.lastCocoon
import net.sbo.mod.utils.Helper.lastInqDeath
import net.sbo.mod.utils.Helper.lastKingDeath
import net.sbo.mod.utils.Helper.lastMantiDeath
import net.sbo.mod.utils.Helper.lastSphinxDeath
import net.sbo.mod.utils.Helper.showTitle
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.Player
import net.sbo.mod.utils.SoundHandler.playCustomSound
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.chat.ChatUtils.formattedString
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.DianaMobDeathEvent
import net.sbo.mod.utils.events.impl.entity.EntityLoadEvent
import net.sbo.mod.utils.events.impl.entity.EntityUnloadEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayExamples
import net.sbo.mod.utils.overlay.OverlayTextLine
import kotlin.math.roundToInt

object DianaMobDetect {
    private const val DEATH_WINDOW_SECONDS = 5
    private const val COCOON_COOLDOWN_MS = 10_000L
    private const val CHAT_DELAY_MS = 200L
    private const val ANNOUNCE_DELAY_MS = 5_000L

    private const val NAME_CHECK_TIMEOUT_MS = 1000L

    private val COCOON_TEXTURE = "eyJ0aW1lc3RhbXAiOjE1ODMxMjMyODkwNTMsInByb2ZpbGVJZCI6IjkxZjA0ZmU5MGYzNjQzYjU4ZjIwZTMzNzVmODZkMzllIiwicHJvZmlsZU5hbWUiOiJTdG9ybVN0b3JteSIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGNlYjBlZDhmYzIyNzJiM2QzZDgyMDY3NmQ1MmEzOGU3YjJlOGRhOGM2ODdhMjMzZTBkYWJhYTE2YzBlOTZkZiJ9fX0="
    private val healthRegex = """([0-9]+(?:\.[0-9]+)?[MK]?)§f/""".toRegex()

    private val unconfirmed = mutableMapOf<Int, Pair<ArmorStandEntity, Long>>()
    private val tracked = mutableMapOf<Int, ArmorStandEntity>()
    private val defeated = mutableSetOf<Int>()
    private val warned = mutableSetOf<Int>()

    private val mobHpOverlay: Overlay = Overlay("mythosMobHp", 10f, 10f, 1f, listOf("Chat screen"), OverlayExamples.mythosMobHpExample).setCondition { Diana.mythosMobHp }
    private val noShurikenOverlay: Overlay = Overlay("noShuriken", 10f, 10f, 3f, listOf("Chat screen"), OverlayExamples.dianaStarlessMobExample).setCondition { Diana.noShurikenOverlay }

    internal enum class RareDianaMob(val display: String) {
        INQ("Minos Inquisitor"),
        KING("King Minos"),
        SPHINX("Sphinx"),
        MANTI("Manticore");

        companion object {
            fun fromName(name: String): RareDianaMob? = entries.firstOrNull { name.contains(it.display, ignoreCase = true) }
        }
    }



    private fun parseHealthFromName(name: String): Double? =
        healthRegex.find(name)?.groupValues?.get(1)?.let { raw ->
            when {
                raw.endsWith("M") -> raw.dropLast(1).toDoubleOrNull()?.times(1_000_000)
                raw.endsWith("K") -> raw.dropLast(1).toDoubleOrNull()?.times(1_000)
                else -> raw.toDoubleOrNull()
            }
        }

    private fun parseStarFromName(name: String): Boolean = name.contains("✯")//todo: implement overlay for star check

    private fun shouldAlertForMob(name: String) = RareDianaMob.fromName(name) != null && Diana.hpAlert > 0.0

    fun init() {
        mobHpOverlay.init()
        noShurikenOverlay.init()
        Register.onTick(1) {
            val world = mc.world ?: return@onTick
            val player = mc.player ?: return@onTick
            val overlayLines = mutableListOf<OverlayTextLine>()

            val unconfirmedIterator = unconfirmed.iterator()
            val now = System.currentTimeMillis()

            while (unconfirmedIterator.hasNext()) {
                val (id, data) = unconfirmedIterator.next()
                val (entity, spawnTime) = data

                //#if MC >= 1.21.9
                //$$ val entityWorld = entity.entityWorld
                //#else
                val entityWorld = entity.world
                //#endif
                if (!entity.isAlive || entityWorld != world) {
                    unconfirmedIterator.remove()
                    continue
                }

                checkCocoon(entity)

                val name = entity.customName?.formattedString() ?: entity.name.formattedString()
                if (name.contains("§2✿", ignoreCase = true)) {
                    tracked[id] = entity
                    unconfirmedIterator.remove()
                }
                else if (now - spawnTime > NAME_CHECK_TIMEOUT_MS) unconfirmedIterator.remove()
            }

            var closestStarlessMob: ArmorStandEntity? = null
            var closestDistanceSq = Double.MAX_VALUE
            val trackedIterator = tracked.iterator()
            while (trackedIterator.hasNext()) {
                val (id, armorStand) = trackedIterator.next()

                //#if MC >= 1.21.9
                //$$ val entityWorld = armorStand.entityWorld
                //#else
                val entityWorld = armorStand.world
                //#endif
                if (!armorStand.isAlive || entityWorld != world) {
                    trackedIterator.remove()
                    defeated.remove(id)
                    continue
                }

                checkDianaMob(armorStand, id)?.let { overlayLines.add(it) }

                val result = checkStarlessMob(armorStand, id, player, closestStarlessMob, closestDistanceSq)
                closestStarlessMob = result.first
                closestDistanceSq = result.second
            }
            mobHpOverlay.setLines(overlayLines)

            if (closestStarlessMob != null) {
                noShurikenOverlay.setLines(listOf(
                    OverlayTextLine("§c§lNO SHURIKEN!"),
                ))
            } else {
                noShurikenOverlay.clearLines()
            }
        }
    }

    @SboEvent
    fun onEntityLoad(event: EntityLoadEvent) {
        if (event.entity is ArmorStandEntity) {
            unconfirmed[event.entity.id] = event.entity to System.currentTimeMillis()
        }
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        if (event.entity is ArmorStandEntity) {
            tracked.remove(event.entity.id)
            defeated.remove(event.entity.id)
        }
    }

    private fun checkDianaMob(entity: ArmorStandEntity, id: Int) : OverlayTextLine? {
        val name = entity.customName?.formattedString() ?: entity.name.formattedString()
        if (name.isEmpty() || name == "Armor Stand") return null
        if (!name.contains("§2✿", ignoreCase = true)) return null

        val health = parseHealthFromName(name)
        maybeTriggerHealthAlert(name, id, health)

        if (health != null && health <= 0.0 && id !in defeated) {
            defeated.add(id)
            warned.remove(id)
            SBOEvent.emit(DianaMobDeathEvent(name, entity))
        }
        return OverlayTextLine(name)
    }

    private fun checkStarlessMob(
        entity: ArmorStandEntity,
        id: Int,
        player: PlayerEntity,
        currentClosest: ArmorStandEntity?,
        currentDistanceSq: Double
    ): Pair<ArmorStandEntity?, Double> {
        if (id in defeated) return currentClosest to currentDistanceSq
        val name = entity.customName?.formattedString() ?: entity.name.formattedString()
        val mobType = RareDianaMob.fromName(name) ?: return currentClosest to currentDistanceSq

        val shouldCheck = when (mobType) {
            RareDianaMob.INQ -> Diana.NoShurikenList.INQ in Diana.NoShurikenMobs
            RareDianaMob.MANTI -> Diana.NoShurikenList.MANTICORE in Diana.NoShurikenMobs
            RareDianaMob.KING -> Diana.NoShurikenList.KING in Diana.NoShurikenMobs
            RareDianaMob.SPHINX -> Diana.NoShurikenList.SPHINX in Diana.NoShurikenMobs
        }

        if (!shouldCheck) return currentClosest to currentDistanceSq
        if (parseStarFromName(name)) return currentClosest to currentDistanceSq

        val health = parseHealthFromName(name)
        if (health != null && health <= 0.0) return currentClosest to currentDistanceSq

        val distSq = player.squaredDistanceTo(entity)
        return if (distSq < currentDistanceSq) {
            entity to distSq
        } else {
            currentClosest to currentDistanceSq
        }
    }

    private fun checkCocoon(entity: ArmorStandEntity) {
        if (World.getWorld() != "Hub") return
        val recentDeath = listOf(lastInqDeath, lastKingDeath, lastSphinxDeath, lastMantiDeath)
            .any { getSecondsPassed(it) < DEATH_WINDOW_SECONDS }

        if (!recentDeath) return
        val head: ItemStack = entity.getEquippedStack(EquipmentSlot.HEAD)

        if (head.isEmpty) return
        if (head.item.toString() != "minecraft:player_head") return
        //#if MC >= 1.21.9
        //$$ val profileComponent: ProfileComponent? = head.get(DataComponentTypes.PROFILE)
        //$$ val texture: String? = profileComponent?.getGameProfile()?.properties?.get("textures")?.firstOrNull()?.value
        //#else
        val profile: ProfileComponent? = head.get(DataComponentTypes.PROFILE)
        val texture: String? = profile?.properties?.get("textures")?.firstOrNull()?.value
        //#endif

        if (texture == null) return
        val now = System.currentTimeMillis()
        if (texture == COCOON_TEXTURE && lastCocoon + COCOON_COOLDOWN_MS < now) {
            lastCocoon = now
            if (Diana.announceCocoon) {
                sleep(CHAT_DELAY_MS) { Chat.command("pc Cocoon!") }
            }
            if (Diana.cocoonTitle) {
                showTitle("§r§6§l<§b§l§kO§6§l> §b§lCOCOON! §6§l<§b§l§kO§6§l>", null, 10, 40, 10)
                playCustomSound(Customization.cocoonSound[0], Customization.cocoonVolume)
            }
        }
    }

    private fun maybeTriggerHealthAlert(name: String, id: Int, health: Double?) {
        if (health == null) return
        if (id in defeated || id in warned) return
        if (!shouldAlertForMob(name)) return
        val hpThreshold = if (Diana.hpAlert > 0.0) Diana.hpAlert * 1_000_000 else 0.0
        if (hpThreshold > 0.0 && health <= hpThreshold) {
            showTitle("§cHP LOW!", null, 10, 40, 10)
            warned.add(id)
        }
    }

    fun onRareSpawn(mob: String) {
        if (Diana.shareRareMob) {
            val mobType = when (mob) {
                RareDianaMob.INQ.display -> Diana.ShareList.INQ
                RareDianaMob.KING.display -> Diana.ShareList.KING
                RareDianaMob.SPHINX.display -> Diana.ShareList.SPHINX
                RareDianaMob.MANTI.display -> Diana.ShareList.MANTICORE
                else -> null
            } ?: return

            if (mobType !in Diana.ShareMobs) return
            val playerPos = Player.getLastPosition()
            Chat.command("pc x: ${playerPos.x.roundToInt()}, y: ${playerPos.y.roundToInt() - 1}, z: ${playerPos.z.roundToInt()} | $mob")
        }

        when (mob) {
            RareDianaMob.INQ.display -> {
                Diana.announceInqText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceInqText[0], "inq")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.command("pc $message") }
                    }
                }
            }

            RareDianaMob.SPHINX.display -> {
                Diana.announceSphinxText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceSphinxText[0], "sphinx")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.command("pc $message") }
                    }
                }
            }

            RareDianaMob.MANTI.display -> {
                Diana.announceMantiText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceMantiText[0], "manti")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.command("pc $message") }
                    }
                }
            }

            RareDianaMob.KING.display -> {
                Diana.announceKingText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceKingText[0], "king")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.command("pc $message") }
                    }
                }
            }
        }

    }
}