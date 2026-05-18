package net.sbo.mod.diana

import net.minecraft.client.multiplayer.ClientLevel
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

/**
 * Highlights rare Diana mobs by making them glow.
 *
 * This object listens for entity load and unload events to track rare mobs.
 * On every 4th tick, it checks if the rare mobs should be glowing based on visibility
 * and settings, and updates their glow state accordingly.
 */
object RareMobHighlight {
    private val rareMobs = mutableSetOf<Player>()

    fun init() {
        Register.onTick(4) {
            val world = mc.level ?: return@onTick
            world.checkMobGlow()
        }
    }

    @SboEvent
    fun onEntityLoad(event: EntityLoadEvent) {
        if (event.entity is Player) {
            if (!Diana.HighlightRareMobs) return
            if (event.entity.uuid.version() == 4) return
            if (RareDianaMob.entries.any { event.entity.name.string.contains(it.display, ignoreCase = true) }) {
                rareMobs.add(event.entity)
            }
        }
    }

    @SboEvent
    fun onEntityUnload(event: EntityUnloadEvent) {
        if (event.entity is Player) {
            if (!Diana.HighlightRareMobs) return
            if (rareMobs.contains(event.entity)) {
                event.entity.isSboGlowing = false
                rareMobs.remove(event.entity)
            }
        }
    }

    private fun ClientLevel.checkMobGlow() {
        val iterator = rareMobs.iterator()
        while (iterator.hasNext()) {
            val mob = iterator.next()

            val entityWorld = mob.level()
            if (!mob.isAlive || entityWorld != this) {
                mob.isSboGlowing = false
                iterator.remove()
                continue
            }

            if (Diana.HighlightRareMobs && mc.player?.hasLineOfSight(mob) == true && !mob.isInvisible) {
                mob.isSboGlowing = true
                mob.setSboGlowColor(Color(Diana.HighlightColor))
            } else {
                mob.isSboGlowing = false
            }
        }
    }
}