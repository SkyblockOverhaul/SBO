package net.sbo.mod.diana

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.diana.DianaMobDetect.RareDianaMob
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.accessors.isSboGlowing
import net.sbo.mod.utils.accessors.setSboGlowColor
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.EntityLoadEvent
import net.sbo.mod.utils.events.impl.entity.EntityUnloadEvent
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
//#if MC > 26.1
//$$ import net.minecraft.util.ARGB
//$$ import net.azureaaron.renderchest.api.CustomGlowCallback
//$$ import net.azureaaron.renderchest.api.GlowConstants
//#endif

/**
 * Highlights rare Diana mobs by making them glow.
 *
 * This object listens for entity load and unload events to track rare mobs.
 *
 * On 26.2, it uses the RenderChest library to make the glow happen.
 *
 * On 26.1.2, every 4th tick, it checks if the rare mobs should be glowing based on simple line of sight
 * visibility and settings, and updates their glow state accordingly with our in-house mixin.
 */
object RareMobHighlight {
    private val NAME_CHECK_TIMEOUT_NS = TimeUnit.SECONDS.toNanos(1L)

    private val rareMobs = ConcurrentHashMap<LivingEntity, RareDianaMob>()
    private val pendingMobs = ConcurrentHashMap<LivingEntity, Long>()

    fun init() {
        Register.onTick(4) {
            val world = mc.level ?: return@onTick
            world.checkMobGlow()
        }

        // Adapted from https://github.com/hannibal002/SkyHanni/blob/5bbf2d1cc23a58553d7602a29d1a90f4eb94e3d7/src/main/java/at/hannibal2/skyhanni/mixins/hooks/RenderLivingEntityHelper.kt#L36

        //#if MC > 26.1
        //$$ CustomGlowCallback.EVENT.register { entity, _ ->
        //$$    if (!Diana.HighlightRareMobs) GlowConstants.NO_GLOW
        //$$
        //$$    // Minecraft already renders all glow as opaque, and non-opaque values are reserved by Render Chest.
        //$$    rareMobs[entity]?.glowColor?.let(ARGB::opaque) ?: GlowConstants.NO_GLOW
        //$$ }
        //#endif
    }

    @SboEvent
    fun onEntityLoad(event: EntityLoadEvent) {
        val mob = event.entity
        if (mob is LivingEntity) {
            if (!Diana.HighlightRareMobs) return
            if (event.entity is Player && event.entity.uuid.version() == 4) return

            val rare = RareDianaMob.fromName(mob.name.string)
            if (rare != null) {
                rareMobs[mob] = rare
            } else {
                pendingMobs[mob] = System.nanoTime()
            }
        }
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        if (event.entity is LivingEntity) {
            pendingMobs.remove(event.entity)
            if (rareMobs.remove(event.entity) != null) {
                event.entity.isSboGlowing = false
            }
        }
    }

    private fun ClientLevel.checkMobGlow() {
        val pendingIterator = pendingMobs.entries.iterator()
        val now = System.nanoTime()

        while (pendingIterator.hasNext()) {
            val (mob, spawnTime) = pendingIterator.next()

            if (!mob.isAlive || mob.level() != this) {
                pendingIterator.remove()
                continue
            }

            val rare = RareDianaMob.fromName(mob.name.string)
            if (rare != null) {
                rareMobs[mob] = rare
                pendingIterator.remove()
            } else if (now - spawnTime > NAME_CHECK_TIMEOUT_NS) {
                pendingIterator.remove()
            }
        }

        //#if MC < 26.2
        val player = mc.player

        val iterator = rareMobs.entries.iterator()
        while (iterator.hasNext()) {
            val (mob, type) = iterator.next()

            if (!mob.isAlive || mob.level() != this) {
                mob.isSboGlowing = false
                iterator.remove()
                continue
            }

            val hasLineOfSight = player != null && player.hasLineOfSight(mob)
            if (Diana.HighlightRareMobs && hasLineOfSight && !mob.isInvisible) {
                mob.isSboGlowing = true
                mob.setSboGlowColor(Color(type.glowColor))
            } else {
                mob.isSboGlowing = false
            }
        }
        //#endif
    }
}
