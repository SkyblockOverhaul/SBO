package net.sbo.mod.diana

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.sbo.mod.SBOKotlin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.RareMobs
import net.sbo.mod.utils.Helper
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.Helper.showTitle
import net.sbo.mod.utils.Helper.sleep
import net.sbo.mod.utils.SoundHandler.playCustomSound
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.chat.ChatUtils.formattedString
import net.sbo.mod.utils.data.DataManager
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.SBOEvent
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.DianaMobDeathEvent
import net.sbo.mod.utils.events.impl.entity.EntityLoadEvent
import net.sbo.mod.utils.events.impl.entity.EntityUnloadEvent
import net.sbo.mod.utils.overlay.Overlay
import net.sbo.mod.utils.overlay.OverlayExamples
import net.sbo.mod.utils.overlay.OverlayTextLine
import net.sbo.mod.utils.waypoint.WaypointManager
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import net.sbo.mod.utils.Player as SboPlayer

object DianaMobDetect {
    private const val MYTHO_MOB_TYPE_CHAR = ""

    private const val ANNOUNCE_DELAY_MS = 5_000L

    private val NAME_CHECK_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(1L)

    private val healthRegex = """([0-9]+(?:\.[0-9]+)?[MK]?)§f/""".toRegex()

    private data class UnconfirmedMob(val entity: ArmorStand, val spawnTimeNs: Long)

    private val unconfirmed = Int2ObjectOpenHashMap<UnconfirmedMob>()
    private val tracked = Int2ObjectOpenHashMap<ArmorStand>()
    private val defeated = IntOpenHashSet()
    private val warned = IntOpenHashSet()

    private val mobHpOverlay: Overlay = Overlay(name = "mythosMobHp", x = 10f, y = 10f, exampleView = OverlayExamples.mythosMobHpExample).setCondition { RareMobs.mythosMobHp }
    private val noShurikenOverlay: Overlay = Overlay(name = "noShuriken", x = 10f, y = 10f, scale = 3f, exampleView = OverlayExamples.dianaStarlessMobExample).setCondition { RareMobs.noShurikenOverlay }

    private val kingHitsRegex = """(?:§[0-9a-fk-or])*.*?(\d+)\s+Hits.*""".toRegex()

    private var wasInHub = false

    internal enum class RareDianaMob(val display: String, val glowColor: Int) {
        INQ("Minos Inquisitor", RareMobs.MinosInquisitorGlowColor),
        KING("King Minos", RareMobs.KingMinosGlowColor),
        SPHINX("Sphinx", RareMobs.SphinxGlowColor),
        MANTI("Manticore", RareMobs.ManticoreGlowColor);

        companion object {
            fun fromName(name: String): RareDianaMob? {
                // Avoids Iterator object allocation, small optimization.
                for (i in 0 until entries.size) {
                    val mob = entries[i]
                    if (name.contains(mob.display)) return mob
                }
                return null
            }
        }
    }

    internal val rareMobNames: Set<String> = RareDianaMob.entries.mapTo(mutableSetOf()) { it.display }
    private val prefixes = setOf("Empyrean", "Exalted", "Runic", "Venerable", "Stalwart", "Blessed")

    private fun fallbackRemovePrefix(mobName: String): String {
        return prefixes.firstOrNull { mobName.startsWith("$it ") }
            ?.let { mobName.removePrefix("$it ") }
            ?: mobName
    }

    private fun findKingHits(name: String): MatchResult? {
        if (!World.isInHub()) return null
        if (!name.contains("Hits")) return null
        if (name.contains("Jerry")) return null
        return kingHitsRegex.find(name)
    }

    private fun isKingHitPhase(name: String) =
        findKingHits(name) != null

    private fun parseKingHits(name: String) =
        findKingHits(name)?.groupValues?.getOrNull(1)?.toIntOrNull()

    private fun parseHealthFromName(name: String): Double? =
        healthRegex.find(name)?.groupValues?.get(1)?.let { raw ->
            when {
                raw.endsWith("M") -> raw.dropLast(1).toDoubleOrNull()?.times(1_000_000)
                raw.endsWith("K") -> raw.dropLast(1).toDoubleOrNull()?.times(1_000)
                else -> raw.toDoubleOrNull()
            }
        }

    private fun parseStarFromName(name: String): Boolean = name.contains("") || name.contains("✯") // mf rework update changes the shuriken icon to depend on resourcepack. still check for legacy since that's only on alpha and to keep compatibility with RitualPractice

    private fun shouldAlertForMob(name: String) = RareDianaMob.fromName(name) != null && (RareMobs.hpAlert > 0.0 || RareMobs.soundHpAlert > 0.0)

    fun init() {
        mobHpOverlay.init()
        noShurikenOverlay.init()

        Register.command("sbodebugentities") {
            val level = mc.level ?: return@command

            level.entitiesForRendering().forEach { entity ->
                val health = (entity as? LivingEntity)?.let {
                    " with health ${it.health}/${it.maxHealth}"
                } ?: ""

                val passengers = entity.passengers
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ") { passenger ->
                        "${passenger.javaClass.simpleName} (${passenger.name.string})"
                    }
                    ?.let { " with passengers [$it]" }
                    ?: ""

                val rider = entity.vehicle
                    ?.let {
                        " riding ${it.javaClass.simpleName} (${it.name.string})"
                    }
                    ?: ""

                Chat.chat(
                    "§6[SBO] §eEntity with type ${entity.javaClass.simpleName} " +
                    "with name ${entity.name.string}$health$passengers$rider " +
                    "at x=${entity.x},y=${entity.y},z=${entity.z}"
                )
            }
        }

        Register.onChatMessage(
            Regex("^§a§lCAUGHT!.*?You cocooned a (?<name>.+?)!.*$")
        ) { _, matchResult ->
            val name = matchResult.groups["name"]?.value ?: return@onChatMessage
            val cleanName = name.removeFormatting()

            val rare = RareDianaMob.fromName(cleanName)
            val displayName =
                rare?.display ?: fallbackRemovePrefix(cleanName) // rare.display returns already without prefix; otherwise the fallback would remove the prefix

            // Check if cocooned mob is a diana mob by checking if it's either a rare mob or has the diana mob prefix like empyrean
            if (rare != null || prefixes.firstOrNull { cleanName.startsWith("$it ") } != null) {
                // We need this check otherwise it could track a mob that you cocooned but someone else spawned.
                if (DianaTracker.lastSpawnedMob == displayName) {
                    if (rare != null) RareMobKillTime.onCocoonCaught(displayName)
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
            if (!World.isInHub()) {
                if (wasInHub) {
                    wasInHub = false
                    unconfirmed.clear()
                    tracked.clear()
                    defeated.clear()
                    warned.clear()
                    mobHpOverlay.clearLines()
                    noShurikenOverlay.clearLines()
                }

                return@onTick
            }

            wasInHub = true

            val world = mc.level ?: return@onTick
            val player = mc.player ?: return@onTick
            val overlayLines = mutableListOf<OverlayTextLine>()

            val unconfirmedIterator = unconfirmed.int2ObjectEntrySet().fastIterator()
            val now = System.nanoTime()

            while (unconfirmedIterator.hasNext()) {
                val entry = unconfirmedIterator.next()
                val id = entry.intKey
                val data = entry.value
                val entity = data.entity
                val spawnTime = data.spawnTimeNs

                val entityWorld = entity.level()
                if (entityWorld != world || entity.dead || !entity.isAlive) {
                    unconfirmedIterator.remove()
                    continue
                }

                val name = entity.customName?.formattedString() ?: continue
                if (hasMythoMobTypeChar(name) || isKingHitPhase(name)) {
                    tracked[id] = entity
                    unconfirmedIterator.remove()
                }
                else if (now - spawnTime > NAME_CHECK_TIMEOUT_NS) unconfirmedIterator.remove()
            }

            var closestStarlessMob: ArmorStand? = null
            var closestDistanceSq = Double.MAX_VALUE
            val trackedIterator = tracked.int2ObjectEntrySet().fastIterator()

            while (trackedIterator.hasNext()) {
                val entry = trackedIterator.next()
                val id = entry.intKey
                val armorStand = entry.value

                val entityWorld = armorStand.level()
                if (entityWorld != world || armorStand.dead || !armorStand.isAlive) {
                    trackedIterator.remove()
                    defeated.remove(id)
                    continue
                }

                val name = armorStand.customName?.formattedString() ?: continue
                checkDianaMob(armorStand, name, id)?.let { line ->
                    overlayLines.add(line)
                    LootshareDisplay.overlayStatus(armorStand)?.let { overlayLines.add(OverlayTextLine(it, centered = true)) }
                }

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
            unconfirmed[event.entity.id] = UnconfirmedMob(event.entity, System.nanoTime())
        }
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        if (event.entity is ArmorStand) {
            tracked.remove(event.entity.id)
            defeated.remove(event.entity.id)
        }
    }

    private fun hasMythoMobTypeChar(name: String): Boolean = name.contains("§2$MYTHO_MOB_TYPE_CHAR") || name.contains("§2✿")

    private fun checkDianaMob(entity: ArmorStand, name: String, id: Int) : OverlayTextLine? {
        if (name.isEmpty()) return null

        parseKingHits(name)?.let {
            return OverlayTextLine("§6King Minos §7- §5$it Hits", centered = true)
        }

        if (!hasMythoMobTypeChar(name)) return null

        val health = parseHealthFromName(name)
        maybeTriggerHealthAlert(name, id, health)

        if (health != null && health <= 0.0 && id !in defeated) {
            defeated.add(id)
            warned.remove(id)
            SBOEvent.emit(DianaMobDeathEvent(name, entity))
        }
        return OverlayTextLine(name, centered = true)
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
            RareDianaMob.INQ -> RareMobs.INQ in RareMobs.NoShurikenMobs
            RareDianaMob.MANTI -> RareMobs.MANTICORE in RareMobs.NoShurikenMobs
            RareDianaMob.KING -> RareMobs.KING in RareMobs.NoShurikenMobs
            RareDianaMob.SPHINX -> RareMobs.SPHINX in RareMobs.NoShurikenMobs
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
        if (RareMobs.announceCocoon) {
            Chat.pc("[SBO] Cocooned a $mobName!")
        }

        if (RareMobs.cocoonTitle) {
            showTitle("§r§6§l<§b§l§kO§6§l> §b§lCOCOON! §6§l<§b§l§kO§6§l>", "§b$mobName", 10, 40, 10)
            playCustomSound(DataManager.soundSettingsData.cocoonSound, volume = DataManager.soundSettingsData.cocoonVolume)
        }
    }

    private fun maybeTriggerHealthAlert(name: String, id: Int, health: Double?) {
        if (health == null) return
        if (id in defeated || id in warned) return
        if (!shouldAlertForMob(name)) return
        val hpThreshold = if (RareMobs.hpAlert > 0.0) RareMobs.hpAlert * 1_000_000 else 0.0
        val soundHpThreshold = if (RareMobs.soundHpAlert > 0.0) RareMobs.soundHpAlert * 1_000_000 else 0.0

        if (health <= soundHpThreshold && soundHpThreshold > 0.0) {
            when (RareDianaMob.fromName(name)) {
                RareDianaMob.INQ -> playCustomSound(DataManager.soundSettingsData.lowInqHpSound, DataManager.soundSettingsData.lowInqHpVoume)
                RareDianaMob.KING ->  playCustomSound(DataManager.soundSettingsData.lowKingHpSound, DataManager.soundSettingsData.lowKingHpVoume)
                RareDianaMob.SPHINX -> playCustomSound(DataManager.soundSettingsData.lowSphinxHpSound, DataManager.soundSettingsData.lowSphinxHpVoume)
                RareDianaMob.MANTI -> playCustomSound(DataManager.soundSettingsData.lowMantiHpSound, DataManager.soundSettingsData.lowMantiHpVoume)
                else -> {}
            }

            warned.add(id)
        }

        if (health <= hpThreshold && hpThreshold > 0.0) {
            showTitle("§cHP LOW!", null, 10, 40, 10)
            warned.add(id)
        }
    }

    fun onRareSpawn(mob: String) {
        val mobType: String = when (mob) {
            RareDianaMob.INQ.display -> RareMobs.INQ
            RareDianaMob.KING.display -> RareMobs.KING
            RareDianaMob.SPHINX.display -> RareMobs.SPHINX
            RareDianaMob.MANTI.display -> RareMobs.MANTICORE
            else -> null
        } ?: return

        val mobDisplayName = WaypointManager.notifyRareMob("", mobType, true, true)
        val playerPos = SboPlayer.getLastPosition()

        val waypoint = WaypointManager.addRareMobWaypoint("", WaypointManager.floorToGround(playerPos), mobType, mobDisplayName, true)

        if (mobType == RareMobs.SPHINX && waypoint != null) {
            // After spawning, the stale removal can remove Sphinx waypoint for a self spawn, because it's pending question answer. It would then proceed to re-add it from world-scan, which triggered double rare mob title and custom sound. This fixes it by adding the rare mob here earlier and persisting the rare mob waypoint thru the question phase, which makes the world-scan's existing.any check return early, and stale mob removal to not remove this waypoint, preventing duplicate title and sounds.
            waypoint.preventStaleRareMobRemoval = true
        }

        RareDianaMob.entries.firstOrNull { it.display == mob }?.let(LootshareDisplay::onOwnRareSpawn)

        if (RareMobs.shareRareMob) {
            val mobType = when (mob) {
                RareDianaMob.INQ.display -> RareMobs.INQ
                RareDianaMob.KING.display -> RareMobs.KING
                RareDianaMob.SPHINX.display -> RareMobs.SPHINX
                RareDianaMob.MANTI.display -> RareMobs.MANTICORE
                else -> null
            } ?: return

            if (mobType !in RareMobs.ShareMobs) return

            // TODO Once majority of people are on 0.7.2 or above, change it to send with [SBO] prefix in front.
            val message = "x: ${playerPos.x.roundToInt()}, y: ${playerPos.y.roundToInt() - 1}, z: ${playerPos.z.roundToInt()} | $mob"
            if (RareMobs.shareRareMobInPublicChat) Chat.allChat(message) else Chat.pc(message)
        }

        when (mob) {
            RareDianaMob.INQ.display -> announceSpawn(RareMobs.announceInqText, "inq")
            RareDianaMob.SPHINX.display -> announceSpawn(RareMobs.announceSphinxText, "sphinx")
            RareDianaMob.MANTI.display -> announceSpawn(RareMobs.announceMantiText, "manti")
            RareDianaMob.KING.display -> announceSpawn(RareMobs.announceKingText, "king")
        }
    }

    // All lines go out as one message, empty means off
    private fun announceSpawn(lines: Array<String>, mob: String) {
        val text = Helper.joinLines(lines)
        if (text.isEmpty()) return
        val message = Helper.getSpawnMessage(text, mob)
        sleep(ANNOUNCE_DELAY_MS) { Chat.pc(message) }
    }

    /**
     * Returns confirmed, active rare Diana mob nametag armor stands.
     */
    internal fun getTrackedRareMobs(): List<Pair<ArmorStand, RareDianaMob>> {
        return tracked.values.mapNotNull { stand ->
            val name = stand.customName?.formattedString() ?: return@mapNotNull null
            val mobType = RareDianaMob.fromName(name) ?: return@mapNotNull null

            val receiveType = when (mobType) {
                RareDianaMob.INQ -> RareMobs.INQ
                RareDianaMob.KING -> RareMobs.KING
                RareDianaMob.SPHINX -> RareMobs.SPHINX
                RareDianaMob.MANTI -> RareMobs.MANTICORE
            }

            if (receiveType !in RareMobs.ReceiveMobs) return@mapNotNull null

            stand to mobType
        }
    }

    internal fun getTrackedRareMobEntities(): List<Pair<ArmorStand, String>> =
        tracked.values.mapNotNull { stand ->
            val name = stand.customName?.formattedString() ?: return@mapNotNull null
            val mob = RareDianaMob.fromName(name) ?: return@mapNotNull null
            stand to mob.display
        }

    internal fun getKnownDianaMobEntityIds(): IntOpenHashSet =
        IntOpenHashSet(tracked.size + unconfirmed.size).apply {
            addAll(tracked.keys)
            addAll(unconfirmed.keys)
        }
}
