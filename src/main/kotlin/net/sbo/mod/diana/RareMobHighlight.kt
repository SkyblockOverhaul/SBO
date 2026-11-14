package net.sbo.mod.diana

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.client.player.RemotePlayer
import net.minecraft.world.entity.decoration.Mannequin
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.RareMobs
import net.sbo.mod.utils.Helper.isValidEntityInHub
import net.sbo.mod.utils.accessors.isSboGlowing
//#if MC < 26.2
import net.sbo.mod.utils.accessors.setSboGlowColor
//#endif
import net.sbo.mod.utils.events.Register
import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.entity.EntityLoadEvent
import net.sbo.mod.utils.events.impl.entity.EntityUnloadEvent
//#if MC < 26.2
import java.awt.Color
//#endif
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

    private val rareMobs = mutableMapOf<LivingEntity, RareMob>()
    private val pendingMobs = Object2LongOpenHashMap<LivingEntity>()

    fun init() {
        Register.onTick(1) {
            val world = mc.level ?: return@onTick
            world.checkMobGlow()
        }

        // Adapted from https://github.com/hannibal002/SkyHanni/blob/5bbf2d1cc23a58553d7602a29d1a90f4eb94e3d7/src/main/java/at/hannibal2/skyhanni/mixins/hooks/RenderLivingEntityHelper.kt#L36

        //#if MC > 26.1
        //$$ CustomGlowCallback.EVENT.register { entity, _ ->
        //$$    if (!RareMobs.HighlightRareMobs) {
        //$$        return@register GlowConstants.NO_GLOW
        //$$    }
        //$$
        //$$    val rareMob = rareMobs[entity]
        //$$    if (rareMob == null) {
        //$$        return@register GlowConstants.NO_GLOW
        //$$    }
        //$$
        //$$    // Minecraft already renders all glow as opaque, and non-opaque values are reserved by Render Chest.
        //$$    val color = ARGB.opaque(rareMob.glowColor)
        //$$
        //$$    return@register color
        //$$ }
        //#endif
    }

    @SboEvent
    fun onEntityLoad(event: EntityLoadEvent) {
        if (!RareMobs.HighlightRareMobs) return

        val mob = event.entity
        val isPlayer = mob is RemotePlayer

        if (isPlayer || mob is Mannequin) {
            if (isPlayer && mob.uuid.version() == 4) return
            if (!mob.isValidEntityInHub(checkInvisibility = false)) return

            val rare = RareMob.fromName(mob.name.string)

            if (rare != null) {
                rareMobs[mob] = rare
            } else {
                pendingMobs[mob] = System.nanoTime()
            }
        }
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        val mob = event.entity

        if (mob is RemotePlayer || mob is Mannequin) {
            pendingMobs.removeLong(mob)

            if (rareMobs.remove(mob) != null) {
                mob.isSboGlowing = false
            }
        }
    }

    private fun ClientLevel.checkMobGlow() {
        val player = mc.player

        val pendingIterator = pendingMobs.object2LongEntrySet().fastIterator()
        val now = System.nanoTime()

        while (pendingIterator.hasNext()) {
            val entry = pendingIterator.next()
            val mob = entry.key
            val spawnTime = entry.longValue

            if (!RareMobs.HighlightRareMobs || !mob.isValidEntityInHub(checkInvisibility = false)) {
                pendingIterator.remove()
                continue
            }

            val rare = RareMob.fromName(mob.name.string)
            if (rare != null) {
                rareMobs[mob] = rare
                pendingIterator.remove()
            } else if (now - spawnTime > NAME_CHECK_TIMEOUT_NS) {
                pendingIterator.remove()
            }
        }

        val iterator = rareMobs.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val mob = entry.key
            //#if MC < 26.2
            val type = entry.value
            //#endif

            if (!RareMobs.HighlightRareMobs || player == null || !mob.isValidEntityInHub(checkInvisibility = false)) {
                mob.isSboGlowing = false
                iterator.remove()
                continue
            }

            //#if MC < 26.2
            if (player.hasLineOfSight(mob) && !mob.isInvisible) {
                mob.isSboGlowing = true
                mob.setSboGlowColor(Color(type.glowColor))
            } else {
                mob.isSboGlowing = false
            }
            //#endif
        }
    }
}
