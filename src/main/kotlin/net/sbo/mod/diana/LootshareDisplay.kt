package net.sbo.mod.diana

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EntityAttachment
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaMobDetect.RareDianaMob
import net.sbo.mod.partyfinder.PartyFinderManager
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.Helper.removeFormatting
import net.sbo.mod.utils.chat.Chat
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.EntitiyHitEvent
import net.sbo.mod.utils.events.impl.entity.EntityLoadEvent
import net.sbo.mod.utils.events.impl.entity.EntityUnloadEvent
import net.sbo.mod.utils.render.RenderUtils3D
import java.util.concurrent.TimeUnit

/**
 * Shows "Lootshared" above rare Diana mobs while in a party: grey and struck through until you dealt
 * enough damage for loot share (1% of the mob's max HP), aqua after that. Mobs you spawned yourself are skipped.
 *
 * Damage is counted from the damage splash armor stands that appear right after you hit the mob, like Skysoft does.
 *
 * The label follows the real mob entity at its interpolated render position instead of the name armor stand,
 * so it does not lag behind when the mob moves. Only its height comes from the name stand, smoothed over time.
 */
object LootshareDisplay : LevelRenderEvents.CollectSubmits {
    private const val LOOTSHARE_DAMAGE_FRACTION = 0.01
    private const val NAME_STAND_MAX_ID_OFFSET = 5
    private const val NAME_STAND_MAX_DISTANCE_SQ = 6.0 * 6.0
    private const val SPLASH_MAX_DISTANCE_SQ = 5.0 * 5.0
    private const val OWN_SPAWN_MAX_DISTANCE_SQ = 20.0 * 20.0
    private const val LABEL_HEIGHT_SMOOTHING = 0.1
    private const val LABEL_GAP = 0.03
    // Where vanilla puts a name tag on top of an entity, used until the name stand is found
    private const val DEFAULT_NAME_TAG_HEIGHT = 0.5
    private val NAME_CHECK_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(1L)
    private val ATTACK_WINDOW_NS = TimeUnit.MILLISECONDS.toNanos(900L)
    private val OWN_SPAWN_WINDOW_NS = TimeUnit.SECONDS.toNanos(5L)
    private val RESPAWN_WINDOW_NS = TimeUnit.SECONDS.toNanos(3L)
    private const val RESPAWN_MAX_DISTANCE_SQ = 8.0 * 8.0

    private val healthRegex = """([0-9.,]+[KMB]?)/([0-9.,]+[KMB]?)❤""".toRegex()
    // Adapted from https://github.com/Akinsoft/Skysoft/blob/main/src/main/kotlin/com/skysoft/features/combat/DamageSplashText.kt
    private val damageSplashRegex = """^[✧✯]?([0-9.]+[KMBkmb]?)[⚔+✧❤♞☄✷ﬗ✯]*$""".toRegex()

    private val notLootshared: Component = Component.literal("Lootshared").withStyle { it.withColor(0xAAAAAA).withStrikethrough(true) }
    private val lootshared: Component = Component.literal("Lootshared").withStyle { it.withColor(0x55FFFF) }

    private class Target(val mob: LivingEntity, val type: RareDianaMob, var own: Boolean) {
        var nameStand: ArmorStand? = null
        var maxHealth: Double? = null
        var damage = 0.0
        var lastAttackNanos = 0L
        var announced = false
        var removedNanos = 0L
        var labelHeight = mob.bbHeight + DEFAULT_NAME_TAG_HEIGHT
        var hasLabelHeight = false

        // Debug test mode also labels your own mobs
        val isLabeled: Boolean
            get() = !own || Debug.lootshareDisplayTest

        val isLootshared: Boolean
            get() = maxHealth?.let { damage >= it * LOOTSHARE_DAMAGE_FRACTION } == true

        fun attackedWithin(nanos: Long) = lastAttackNanos != 0L && nanos - lastAttackNanos in 0..ATTACK_WINDOW_NS
    }

    private val targets = mutableMapOf<Int, Target>()
    private val pendingMobs = mutableMapOf<LivingEntity, Long>()
    private val pendingSplashes = mutableMapOf<ArmorStand, Long>()
    // Targets whose entity just went away, in case the server replaces the mob with a new entity right after spawning
    private val removedTargets = mutableListOf<Target>()
    private var ownSpawn: RareDianaMob? = null
    private var ownSpawnNanos = 0L

    fun init() {
        Register.onTick(1) {
            if (!Diana.lootshareDisplay) {
                clear()
                return@onTick
            }
            val world = mc.level ?: return@onTick
            val now = System.nanoTime()

            checkPendingMobs(now)

            targets.values.toList().forEach { if (!it.mob.isAlive || it.mob.level() != world) removeTarget(it.mob.id) }
            removedTargets.removeIf { now - it.removedNanos > RESPAWN_WINDOW_NS }
            targets.values.forEach { it.updateNameStand() }

            checkPendingSplashes(now)
            targets.values.forEach { it.announceLootshare() }
        }

        LevelRenderEvents.COLLECT_SUBMITS.register(this)
    }

    /** Called when you spawn a rare mob, so the next one of that type showing up near you is not labeled. */
    internal fun onOwnRareSpawn(type: RareDianaMob) {
        ownSpawn = type
        ownSpawnNanos = System.nanoTime()
    }

    private fun clear() {
        targets.clear()
        pendingMobs.clear()
        pendingSplashes.clear()
        removedTargets.clear()
    }

    private fun removeTarget(id: Int) {
        val target = targets.remove(id) ?: return
        target.removedNanos = System.nanoTime()
        removedTargets.add(target)
    }

    /** Rare mobs around you right now, including your own, for the party overlay. */
    internal val rareMobsAround: List<RareDianaMob> get() = targets.values.map { it.type }

    /** Whether you spawned one of the rare mobs around you, the debug test mode treats them as someone else's. */
    val ownMobAround: Boolean get() = targets.values.any { it.own && !Debug.lootshareDisplayTest }

    val lootsharedAny: Boolean get() = targets.values.any { it.isLabeled && it.isLootshared }

    /** Whether labels and overlay status are shown right now. */
    private fun isShown() = Diana.lootshareDisplay && (PartyFinderManager.isInParty || Debug.lootshareDisplayTest)

    /** Lootshare status line shown under the mythos mob overlay line of [nameStand], null when that mob has no label. */
    fun overlayStatus(nameStand: ArmorStand): String? {
        if (!isShown()) return null
        val target = targets.values.firstOrNull { it.nameStand == nameStand && it.isLabeled } ?: return null
        return if (target.isLootshared) "§bLootshared" else "§7§mLootshared"
    }

    @SboEvent
    fun onEntityLoad(event: EntityLoadEvent) {
        if (!Diana.lootshareDisplay) return
        val entity = event.entity
        if (entity is ArmorStand) {
            // Damage splashes get their name a moment after spawning, so check them over the next ticks
            if (targets.values.any { it.lastAttackNanos != 0L }) pendingSplashes[entity] = System.nanoTime()
            return
        }
        if (entity !is LivingEntity) return
        if (entity is Player && entity.uuid.version() == 4) return
        pendingMobs[entity] = System.nanoTime()
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        removeTarget(event.entity.id)
        pendingMobs.remove(event.entity)
        pendingSplashes.remove(event.entity)
    }

    @SboEvent
    fun onEntityHit(event: EntitiyHitEvent) {
        if (event.player != mc.player) return
        // Resolve a mob that was hit before its first tick here, so an instant hit still counts
        if (event.entity in pendingMobs) checkPendingMobs(System.nanoTime())
        val target = targets[event.entity.id] ?: targets.values.firstOrNull { it.nameStand == event.entity } ?: return
        target.lastAttackNanos = System.nanoTime()
    }

    private fun checkPendingMobs(now: Long) {
        val iterator = pendingMobs.entries.iterator()
        while (iterator.hasNext()) {
            val (mob, loadNanos) = iterator.next()
            val type = RareDianaMob.fromName(mob.name.string)
            if (type != null) {
                targets[mob.id] = Target(mob, type, consumeOwnSpawn(type, mob, now)).also { it.takeOverRemoved(now) }
                iterator.remove()
            } else if (!mob.isAlive || now - loadNanos > NAME_CHECK_TIMEOUT_NS) {
                iterator.remove()
            }
        }
    }

    private fun consumeOwnSpawn(type: RareDianaMob, mob: LivingEntity, now: Long): Boolean {
        val player = mc.player ?: return false
        if (ownSpawn != type || now - ownSpawnNanos > OWN_SPAWN_WINDOW_NS) return false
        if (player.distanceToSqr(mob) > OWN_SPAWN_MAX_DISTANCE_SQ) return false
        ownSpawn = null
        return true
    }

    // Keep damage dealt to the old entity when the same mob shows up again as a new entity
    private fun Target.takeOverRemoved(now: Long) {
        val old = removedTargets
            .filter { it.type == type && now - it.removedNanos <= RESPAWN_WINDOW_NS && it.mob.distanceToSqr(mob) <= RESPAWN_MAX_DISTANCE_SQ }
            .minByOrNull { it.mob.distanceToSqr(mob) }
            ?: return
        removedTargets.remove(old)
        own = own || old.own
        damage = old.damage
        lastAttackNanos = old.lastAttackNanos
        maxHealth = old.maxHealth
        announced = old.announced
    }

    private fun Target.announceLootshare() {
        if (announced || !isLabeled || !isLootshared) return
        announced = true
        if (!Diana.lootshareMessage) return
        // The debug test mode only tells you, it does not message the party
        if (Debug.lootshareDisplayTest) Chat.chat("§6[SBO] §bLootshared ${type.display}!")
        else if (PartyFinderManager.isInParty) Chat.pc("[SBO] Lootshared ${type.display}!")
    }

    private fun Target.updateNameStand() {
        val stand = nameStand?.takeIf { it.isAlive } ?: findNameStand()?.also { nameStand = it } ?: return
        val match = healthRegex.find(stand.name.string.removeFormatting()) ?: return
        parseNumber(match.groupValues[2])?.let { maxHealth = it }

        // Smooth the height so the trailing stand can't make the label jitter, the position itself comes from the mob
        val nameTag = stand.attachments.get(EntityAttachment.NAME_TAG, 0, stand.yRot)
        val height = stand.y + nameTag.y + DEFAULT_NAME_TAG_HEIGHT - mob.y
        labelHeight = if (hasLabelHeight) labelHeight + (height - labelHeight) * LABEL_HEIGHT_SMOOTHING else height
        hasLabelHeight = true
    }

    // The name stand's entity id is the mob's id ±1 to 5 on Hypixel, see the RareMobHighlight notes
    private fun Target.findNameStand(): ArmorStand? {
        val world = mc.level ?: return null
        for (offset in 1..NAME_STAND_MAX_ID_OFFSET) {
            for (id in intArrayOf(mob.id + offset, mob.id - offset)) {
                val stand = world.getEntity(id) as? ArmorStand ?: continue
                if (stand.distanceToSqr(mob) > NAME_STAND_MAX_DISTANCE_SQ) continue
                if (stand.name.string.contains(type.display, ignoreCase = true)) return stand
            }
        }
        return null
    }

    private fun checkPendingSplashes(now: Long) {
        val iterator = pendingSplashes.entries.iterator()
        while (iterator.hasNext()) {
            val (stand, loadNanos) = iterator.next()
            if (!stand.isAlive || now - loadNanos > NAME_CHECK_TIMEOUT_NS) {
                iterator.remove()
                continue
            }
            val name = stand.customName?.string?.removeFormatting() ?: continue
            val match = damageSplashRegex.matchEntire(name.replace(",", "")) ?: continue
            iterator.remove()
            val damage = parseNumber(match.groupValues[1]) ?: continue

            // Only count splashes right after your own hit, next to the mob you hit
            val target = targets.values
                .filter { it.isLabeled && it.attackedWithin(loadNanos) && stand.distanceToSqr(it.mob) <= SPLASH_MAX_DISTANCE_SQ }
                .minByOrNull { stand.distanceToSqr(it.mob) }
                ?: continue
            target.damage += damage
        }
    }

    private fun parseNumber(raw: String): Double? {
        val value = raw.replace(",", "")
        val multiplier = when (value.lastOrNull()?.uppercaseChar()) {
            'K' -> 1_000.0
            'M' -> 1_000_000.0
            'B' -> 1_000_000_000.0
            else -> return value.toDoubleOrNull()
        }
        return value.dropLast(1).toDoubleOrNull()?.times(multiplier)
    }

    override fun collectSubmits(context: LevelRenderContext) {
        if (!isShown() || targets.isEmpty()) return
        val partialTick = mc.deltaTracker.getGameTimeDeltaPartialTick(true)

        targets.values.forEach { target ->
            if (!target.isLabeled || target.mob.isInvisible) return@forEach
            val pos = target.mob.getPosition(partialTick)
            RenderUtils3D.drawLabel(
                context,
                Vec3(pos.x, pos.y + target.labelHeight + LABEL_GAP, pos.z),
                if (target.isLootshared) lootshared else notLootshared
            )
        }
    }
}
