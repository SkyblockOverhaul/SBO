package net.sbo.mod.diana

import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.Helper.showTitle
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.SoundHandler.playCustomSound
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.chat.ChatUtils.formattedString
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.DianaMobDeathEvent
import net.sbo.mod.utils.events.impl.entity.EntityLoadEvent
import net.sbo.mod.utils.events.impl.entity.EntityUnloadEvent
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayExamples
import net.sbo.mod.utils.overlay.OverlayTextLine
import kotlin.math.roundToInt
import net.sbo.mod.utils.Player as SboPlayer
import java.util.concurrent.TimeUnit

object DianaMobDetect {
    private const val ANNOUNCE_DELAY_MS = 5_000L

    private val NAME_CHECK_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(1L)

    private val healthRegex = """([0-9]+(?:\.[0-9]+)?[MK]?)§f/""".toRegex()

    private val unconfirmed = mutableMapOf<Int, Pair<ArmorStand, Long>>()
    private val tracked = mutableMapOf<Int, ArmorStand>()
    private val defeated = mutableSetOf<Int>()
    private val warned = mutableSetOf<Int>()

    private val mobHpOverlay: Overlay = Overlay(name = "mythosMobHp", x = 10f, y = 10f, exampleView = OverlayExamples.mythosMobHpExample).setCondition { Diana.mythosMobHp }
    private val noShurikenOverlay: Overlay = Overlay(name = "noShuriken", x = 10f, y = 10f, scale = 3f, exampleView = OverlayExamples.dianaStarlessMobExample).setCondition { Diana.noShurikenOverlay }

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

    private val prefixes = listOf("Empyrean", "Exalted", "Runic", "Venerable", "Stalwart", "Blessed")

    private fun fallbackRemovePrefix(mobName: String): String {
        return prefixes.firstOrNull { mobName.startsWith("$it ") }
            ?.let { mobName.removePrefix("$it ") }
            ?: mobName
    }

    fun init() {
        mobHpOverlay.init()
        noShurikenOverlay.init()

        Register.onChatMessage(
            Regex("^§a§lCAUGHT!.*?You cocooned a (?<name>.+?)!.*$")
        ) { _, matchResult ->
            val name = matchResult.groups["name"]?.value ?: return@onChatMessage
            val cleanName = name.removeFormatting()

            val rare = RareDianaMob.fromName(cleanName)
            val displayName =
                rare?.display ?: fallbackRemovePrefix(cleanName) // rare.display returns already without prefix; otherwise the fallback would remove the prefix

            // Check if cocooned mob is a diana mob by checking if it's either a rare mob or has the diana mob prefix like empyrean
            if (rare != null || prefixes.firstOrNull { cleanName.startsWith("$it ")} != null) {
                // We need this check otherwise it could track a mob that you cocooned but someone else spawned.
                if (DianaTracker.lastSpawnedMob == displayName) {
                    // Count as a new spawned mob, exactly as if it was received as spawned by a regular burrow, so that achievements and mob tracking work.
                    // This will give an error in chat if we pass a string that matches no mob name in the tracker handler code.
                    DianaTracker.trackMobOnSpawnAndSave(displayName, fromCocoon = true)
                }
            }

            if (rare != null) {
                // Cocooned a rare mob if rare != null
                announceCocoon(displayName)
            }
        }

        Register.onTick(1) {
            val world = mc.level ?: return@onTick
            val player = mc.player ?: return@onTick
            val overlayLines = mutableListOf<OverlayTextLine>()

            val unconfirmedIterator = unconfirmed.iterator()
            val now = System.nanoTime()

            while (unconfirmedIterator.hasNext()) {
                val (id, data) = unconfirmedIterator.next()
                val (entity, spawnTime) = data

                val entityWorld = entity.level()
                if (!entity.isAlive || entityWorld != world) {
                    unconfirmedIterator.remove()
                    continue
                }

                val name = entity.customName?.formattedString() ?: entity.name.formattedString()
                if (name.contains("§2✿", ignoreCase = true)) {
                    tracked[id] = entity
                    unconfirmedIterator.remove()
                }
                else if (now - spawnTime > NAME_CHECK_TIMEOUT_NS) unconfirmedIterator.remove()
            }

            var closestStarlessMob: ArmorStand? = null
            var closestDistanceSq = Double.MAX_VALUE
            val trackedIterator = tracked.iterator()

            while (trackedIterator.hasNext()) {
                val (id, armorStand) = trackedIterator.next()

                val entityWorld = armorStand.level()
                if (!armorStand.isAlive || entityWorld != world) {
                    trackedIterator.remove()
                    defeated.remove(id)
                    continue
                }

                val name = armorStand.customName?.formattedString() ?: armorStand.name.formattedString()
                checkDianaMob(armorStand, name, id)?.let { overlayLines.add(it) }

                val result = checkStarlessMob(armorStand, name, id, player, closestStarlessMob, closestDistanceSq)
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
        if (event.entity is ArmorStand) {
            unconfirmed[event.entity.id] = event.entity to System.nanoTime()
        }
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        if (event.entity is ArmorStand) {
            tracked.remove(event.entity.id)
            defeated.remove(event.entity.id)
        }
    }

    private fun checkDianaMob(entity: ArmorStand, name: String, id: Int) : OverlayTextLine? {
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
        entity: ArmorStand,
        name: String,
        id: Int,
        player: Player,
        currentClosest: ArmorStand?,
        currentDistanceSq: Double
    ): Pair<ArmorStand?, Double> {
        if (id in defeated) return currentClosest to currentDistanceSq
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

        val distSq = player.distanceToSqr(entity)
        return if (distSq < currentDistanceSq) {
            entity to distSq
        } else {
            currentClosest to currentDistanceSq
        }
    }

    private fun announceCocoon(mobName: String) {
        if (Diana.announceCocoon) {
            Chat.pc("Cocooned a ${mobName}!")
        }

        if (Diana.cocoonTitle) {
            showTitle("§r§6§l<§b§l§kO§6§l> §b§lCOCOON! §6§l<§b§l§kO§6§l>", "§b${mobName}", 10, 40, 10)
            playCustomSound(Customization.cocoonSound[0], Customization.cocoonVolume)
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
            val playerPos = SboPlayer.getLastPosition()
            Chat.pc("x: ${playerPos.x.roundToInt()}, y: ${playerPos.y.roundToInt() - 1}, z: ${playerPos.z.roundToInt()} | $mob")
        }

        when (mob) {
            RareDianaMob.INQ.display -> {
                Diana.announceInqText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceInqText[0], "inq")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.pc(message) }
                    }
                }
            }

            RareDianaMob.SPHINX.display -> {
                Diana.announceSphinxText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceSphinxText[0], "sphinx")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.pc(message) }
                    }
                }
            }

            RareDianaMob.MANTI.display -> {
                Diana.announceMantiText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceMantiText[0], "manti")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.pc(message) }
                    }
                }
            }

            RareDianaMob.KING.display -> {
                Diana.announceKingText.firstOrNull()?.let { killText ->
                    if (killText.isNotBlank()) {
                        val message = Helper.getSpawnMessage(Diana.announceKingText[0], "king")
                        sleep(ANNOUNCE_DELAY_MS) { Chat.pc(message) }
                    }
                }
            }
        }

    }
}
