package net.sbo.mod.diana

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.tracker.Tracker
import net.sbo.mod.settings.categories.RareMobs
import net.sbo.mod.utils.game.World.isValidEntityInHub
import net.sbo.mod.utils.TextUtil.removeFormatting
import net.sbo.mod.utils.chat.Chat.showTitle
import net.sbo.mod.utils.events.TickScheduler.sleep
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
import net.sbo.mod.utils.TextUtil

object DianaMobDetect {
    private const val MYTHO_MOB_TYPE_CHAR = ""

    private const val ANNOUNCE_DELAY_MS = 5_000L

    private val NAME_CHECK_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(1L)

    private data class UnconfirmedMob(val entity: ArmorStand, val spawnTimeNs: Long)

    private val unconfirmed = Int2ObjectOpenHashMap<UnconfirmedMob>()
    private val tracked = Int2ObjectOpenHashMap<ArmorStand>()
    private val defeated = IntOpenHashSet()
    private val warned = IntOpenHashSet()

    private val mobHpOverlay: Overlay = Overlay(name = "mythosMobHp", x = 10f, y = 10f, exampleView = OverlayExamples.mythosMobHpExample).setCondition { RareMobs.mythosMobHp }
    private val noShurikenOverlay: Overlay = Overlay(name = "noShuriken", x = 10f, y = 10f, scale = 3f, exampleView = OverlayExamples.dianaStarlessMobExample).setCondition { RareMobs.noShurikenOverlay }

    private val kingHitsRegex = """(?:§[0-9a-fk-or])*.*?(\d+)\s+Hits.*""".toRegex()

    internal val rareMobNames: Set<String> = RareMob.entries.mapTo(mutableSetOf()) { it.display }
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

    private class HealthCache(private val name: String) {
        private var parsed = false
        private var cached: Double? = null

        fun get(): Double? {
            if (!parsed) {
                cached = parseHealthFromName(name)
                parsed = true
            }

            return cached
        }
    }

    /**
     * Essentially equal to the old version based on healthRegex = """([0-9]+(?:\.[0-9]+)?[MK]?)§f/""".toRegex(), but without the expensive Regex machinery.
     */
    private fun parseHealthFromName(name: String): Double? {
        var i = 0

        while (i < name.length) {
            if (name[i] !in '0'..'9') {
                i++
                continue
            }

            val start = i

            // Integer portion.
            while (i < name.length && name[i] in '0'..'9') {
                i++
            }

            // Optional decimal portion.
            if (i + 1 < name.length && name[i] == '.' && name[i + 1] in '0'..'9') {
                i++

                while (i < name.length && name[i] in '0'..'9') {
                    i++
                }
            }

            val numberEnd = i

            // Optional multiplier.
            val multiplier = when {
                i < name.length && name[i] == 'M' -> {
                    i++
                    1_000_000.0
                }
                i < name.length && name[i] == 'K' -> {
                    i++
                    1_000.0
                }
                else -> 1.0
            }

            if (name.startsWith("§f/", i)) {
                return name.substring(start, numberEnd).toDoubleOrNull()?.times(multiplier)
            }

            // Regex.find also tries matches beginning inside the previous candidate.
            i = start + 1
        }

        return null
    }

    private fun parseStarFromName(name: String): Boolean = name.contains("") || name.contains("✯") // mf rework update changed the shuriken icon to depend on resourcepack. still check for legacy to keep compatibility with RitualPractice

    private fun shouldAlertForMob(name: String) = RareMob.fromName(name) != null && (RareMobs.hpAlert > 0.0 || RareMobs.soundHpAlert > 0.0)

    fun Entity.toDebugString(): String {
        val livingEntity = this as? LivingEntity

        val health = livingEntity?.let {
            " with health ${it.health}/${it.maxHealth}"
        }.orEmpty()

        val dead = livingEntity?.dead

        val passengers = passengers
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { passenger ->
                "${passenger.javaClass.simpleName} (${passenger.name.string})"
            }
            ?.let { " with passengers [$it]" }
            .orEmpty()

        val rider = vehicle
            ?.let {
                " riding ${it.javaClass.simpleName} (${it.name.string})"
            }
            .orEmpty()

        return "Entity with type ${javaClass.simpleName} " +
            "with name ${name.string} " +
            "uuidVersion=${uuid.version()}, " +
            "dead=$dead, " +
            "isAlive=$isAlive, " +
            "isInvisible=$isInvisible" +
            "$health$passengers$rider " +
            "at x=$x,y=$y,z=$z"
    }

    fun init() {
        mobHpOverlay.init()
        noShurikenOverlay.init()

        Register.command("sbodebugentities") {
            val level = mc.level ?: return@command

            level.entitiesForRendering().forEach { entity ->
                Chat.chat("§6[SBO] §e${entity.toDebugString()}")
            }
        }

        Register.onChatMessage(
            Regex("^§a§lCAUGHT!.*?You cocooned a (?<name>.+?)!.*$")
        ) { _, matchResult ->
            val name = matchResult.groups["name"]?.value ?: return@onChatMessage
            val cleanName = name.removeFormatting()

            val rare = RareMob.fromName(cleanName)
            val isRare = rare != null
            val displayName =
                rare?.display ?: fallbackRemovePrefix(cleanName) // rare.display returns already without prefix; otherwise the fallback would remove the prefix

            // Check if cocooned mob is a diana mob by checking if it's either a rare mob or has the diana mob prefix like empyrean, then check if it is same as the last spawned mob of the player. The check is needed because otherwise we could track a rare mob someone else spawned and the player cocooned.
            if ((isRare || prefixes.any { cleanName.startsWith("$it ") }) && Tracker.lastSpawnedMob == displayName) {
                if (isRare) {
                    // Rare mob kill time hook
                    RareMobKillTime.onCocoonCaught(displayName)
                }

                // Count as a new spawned mob, exactly as if it was received as spawned by a regular burrow, so that achievements and mob tracking work.
                // This will give an error in chat if we pass a string that matches no mob name in the tracker handler code.
                Tracker.trackMobOnSpawnAndSave(displayName, fromCocoon = true)
            }

            if (isRare) {
                // Cocooned a rare mob, so announce it
                announceCocoon(displayName)
            }
        }

        Register.onTick(1) {
            val world = mc.level
            val player = mc.player
            val inHub = World.isInHub()

            if (player == null || world == null || !inHub) {
                unconfirmed.clear()
                tracked.clear()
                defeated.clear()
                warned.clear()
                mobHpOverlay.clearLines()
                noShurikenOverlay.clearLines()
                return@onTick
            }

            val unconfirmedIterator = unconfirmed.int2ObjectEntrySet().fastIterator()
            val now = System.nanoTime()

            while (unconfirmedIterator.hasNext()) {
                val entry = unconfirmedIterator.next()
                val id = entry.intKey
                val data = entry.value
                val entity = data.entity
                val spawnTime = data.spawnTimeNs

                if (!entity.isValidEntityInHub(checkInvisibility = false)) {
                    unconfirmedIterator.remove()
                    continue
                }

                val name = entity.customName?.formattedString()
                if (inHub && name != null && (hasMythoMobTypeChar(name) || isKingHitPhase(name))) {
                    tracked[id] = entity
                    unconfirmedIterator.remove()
                } else if (now - spawnTime > NAME_CHECK_TIMEOUT_NS) {
                    unconfirmedIterator.remove()
                }
            }

            val overlayLines = mutableListOf<OverlayTextLine>()

            var closestStarlessMob: ArmorStand? = null
            var closestDistanceSq = Double.MAX_VALUE
            val trackedIterator = tracked.int2ObjectEntrySet().fastIterator()

            while (trackedIterator.hasNext()) {
                val entry = trackedIterator.next()
                val id = entry.intKey
                val armorStand = entry.value

                if (!armorStand.isValidEntityInHub(checkInvisibility = false)) {
                    trackedIterator.remove()
                    warned.remove(id)
                    defeated.remove(id)
                    continue
                }

                val name = armorStand.customName?.formattedString()
                if (name == null) {
                    trackedIterator.remove()
                    warned.remove(id)
                    defeated.remove(id)
                    continue
                }

                val hasMythoType = hasMythoMobTypeChar(name)
                val kingHitsMatch = findKingHits(name)

                if (!hasMythoType && kingHitsMatch == null) {
                    trackedIterator.remove()
                    warned.remove(id)
                    defeated.remove(id)
                    continue
                }

                val health = HealthCache(name)

                checkDianaMob(armorStand, name, id, hasMythoType, kingHitsMatch, health)?.let { line ->
                    overlayLines.add(line)
                    LootshareDisplay.overlayStatus(armorStand)?.let {
                        overlayLines.add(OverlayTextLine(it, centered = true))
                    }
                }

                val result = checkStarlessMob(
                    armorStand,
                    name,
                    id,
                    player,
                    closestStarlessMob,
                    closestDistanceSq,
                    health
                )
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
        val entity = event.entity as? ArmorStand ?: return
        val id = entity.id

        // Remove on spawn, in case a newly-spawned mob has the same id as a previously-despawned mob. IDs are only guaranteed unique in the sense that two loaded entities can not have the same ID - a newly spawned one can get a previously despawned entity's ID.
        unconfirmed.remove(id)
        tracked.remove(id)
        warned.remove(id)
        defeated.remove(id)

        // Missing rare mobs spawned directly after warping to Hub is an acceptable trade-off compared to pointlessly tracking unconfirmed mobs in every other island and risking memory leaks.
        if (World.isInHub()) {
            unconfirmed[id] = UnconfirmedMob(entity, System.nanoTime())
        }
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        if (event.entity is ArmorStand) {
            unconfirmed.remove(event.entity.id)
            tracked.remove(event.entity.id)
            warned.remove(event.entity.id)
            defeated.remove(event.entity.id)
        }
    }

    private fun hasMythoMobTypeChar(name: String): Boolean = name.contains("§2$MYTHO_MOB_TYPE_CHAR") || name.contains("§2✿")

    private fun checkDianaMob(
        entity: ArmorStand,
        name: String,
        id: Int,
        hasMythoType: Boolean,
        kingHitsMatch: MatchResult?,
        health: HealthCache
    ): OverlayTextLine? {
        if (name.isEmpty()) return null

        kingHitsMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            return OverlayTextLine("§6King Minos §7- §5$it Hits", centered = true)
        }

        if (!hasMythoType) return null

        val parsedHealth = health.get()

        if (parsedHealth != null) {
            maybeTriggerHealthAlert(name, id, parsedHealth)

            if (parsedHealth <= 0.0 && id !in defeated) {
                defeated.add(id)
                warned.remove(id)
                SBOEvent.emit(DianaMobDeathEvent(name, entity))
            }
        }

        return OverlayTextLine(name, centered = true)
    }

    private fun checkStarlessMob(
        entity: ArmorStand,
        name: String,
        id: Int,
        player: Player,
        currentClosest: ArmorStand?,
        currentDistanceSq: Double,
        health: HealthCache
    ): Pair<ArmorStand?, Double> {
        if (id in defeated) return currentClosest to currentDistanceSq
        val mobType = RareMob.fromName(name) ?: return currentClosest to currentDistanceSq

        val shouldCheck = mobType in RareMobs.NoShurikenMobs

        if (!shouldCheck) return currentClosest to currentDistanceSq
        if (parseStarFromName(name)) return currentClosest to currentDistanceSq

        val parsedHealth = health.get()
        if (parsedHealth != null && parsedHealth <= 0.0) return currentClosest to currentDistanceSq

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

    private fun maybeTriggerHealthAlert(name: String, id: Int, health: Double) {
        if (id in defeated || id in warned) return
        if (!shouldAlertForMob(name)) return
        val hpThreshold = if (RareMobs.hpAlert > 0.0) RareMobs.hpAlert * 1_000_000 else 0.0
        val soundHpThreshold = if (RareMobs.soundHpAlert > 0.0) RareMobs.soundHpAlert * 1_000_000 else 0.0

        if (health <= soundHpThreshold && soundHpThreshold > 0.0) {
            when (RareMob.fromName(name)) {
                RareMob.INQ -> playCustomSound(DataManager.soundSettingsData.lowInqHpSound, DataManager.soundSettingsData.lowInqHpVolume)
                RareMob.KING ->  playCustomSound(DataManager.soundSettingsData.lowKingHpSound, DataManager.soundSettingsData.lowKingHpVolume)
                RareMob.SPHINX -> playCustomSound(DataManager.soundSettingsData.lowSphinxHpSound, DataManager.soundSettingsData.lowSphinxHpVolume)
                RareMob.MANTICORE -> playCustomSound(DataManager.soundSettingsData.lowMantiHpSound, DataManager.soundSettingsData.lowMantiHpVolume)
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
        val mobType: RareMob = when (mob) {
            RareMob.INQ.display -> RareMob.INQ
            RareMob.KING.display -> RareMob.KING
            RareMob.SPHINX.display -> RareMob.SPHINX
            RareMob.MANTICORE.display -> RareMob.MANTICORE
            else -> null
        } ?: return

        val mobDisplayName = WaypointManager.notifyRareMob("", mobType, true, true)

        val playerPos = SboPlayer.getLastPosition()
        val groundedPos = WaypointManager.floorToGround(playerPos)

        val waypoint = WaypointManager.addRareMobWaypoint("", groundedPos, mobType, mobDisplayName, true)

        if (mobType == RareMob.SPHINX && waypoint != null) {
            // After spawning, the stale removal can remove Sphinx waypoint for a self spawn, because it's pending question answer. It would then proceed to re-add it from world-scan, which triggered double rare mob title and custom sound. This fixes it by adding the rare mob here earlier and persisting the rare mob waypoint through the question phase, which makes the world-scan's existing.any check return early, and stale mob removal to not remove this waypoint, preventing duplicate title and sounds.
            waypoint.preventStaleRareMobRemoval = true
        }

        RareMob.entries.firstOrNull { it.display == mob }?.let(LootshareDisplay::onOwnRareSpawn)

        if (RareMobs.shareRareMob) {
            val mobType = when (mob) {
                RareMob.INQ.display -> RareMob.INQ
                RareMob.KING.display -> RareMob.KING
                RareMob.SPHINX.display -> RareMob.SPHINX
                RareMob.MANTICORE.display -> RareMob.MANTICORE
                else -> null
            } ?: return

            if (mobType !in RareMobs.ShareMobs) return

            // TODO Once majority of people are on 0.7.2 or above, change it to send with [SBO] prefix in front.
            val message = "x: ${groundedPos.x.roundToInt()}, y: ${groundedPos.y.roundToInt()}, z: ${groundedPos.z.roundToInt()} | $mob"
            if (RareMobs.shareRareMobInPublicChat) Chat.allChat(message) else Chat.pc(message)
        }

        when (mob) {
            RareMob.INQ.display -> announceSpawn(RareMobs.announceInqText, "inq")
            RareMob.SPHINX.display -> announceSpawn(RareMobs.announceSphinxText, "sphinx")
            RareMob.MANTICORE.display -> announceSpawn(RareMobs.announceMantiText, "manti")
            RareMob.KING.display -> announceSpawn(RareMobs.announceKingText, "king")
        }
    }

    // All lines go out as one message, empty means off
    private fun announceSpawn(lines: Array<String>, mob: String) {
        val text = TextUtil.joinLines(lines)
        if (text.isEmpty()) return
        val message = DropMessages.getSpawnMessage(text, mob)
        sleep(ANNOUNCE_DELAY_MS) { Chat.pc(message) }
    }

    /**
     * Returns confirmed, active rare Diana mob nametag armor stands.
     */
    internal fun getTrackedRareMobs(): List<Pair<ArmorStand, RareMob>> {
        return tracked.values.mapNotNull { stand ->
            val name = stand.customName?.formattedString() ?: return@mapNotNull null
            val mobType = RareMob.fromName(name) ?: return@mapNotNull null

            if (mobType !in RareMobs.ReceiveMobs) return@mapNotNull null

            stand to mobType
        }
    }

    internal fun getTrackedRareMobEntities(): List<Pair<ArmorStand, String>> =
        tracked.values.mapNotNull { stand ->
            val name = stand.customName?.formattedString() ?: return@mapNotNull null
            val mob = RareMob.fromName(name) ?: return@mapNotNull null
            stand to mob.display
        }

    internal fun getKnownDianaMobEntityIds(): IntOpenHashSet =
        IntOpenHashSet(tracked.size + unconfirmed.size).apply {
            addAll(tracked.keys)
            addAll(unconfirmed.keys)
        }
}
