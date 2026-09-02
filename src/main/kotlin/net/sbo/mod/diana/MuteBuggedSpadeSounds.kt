package net.sbo.mod.diana

import net.sbo.mod.utils.events.annotations.SboEvent
import net.sbo.mod.utils.events.impl.game.PlaySoundEvent
import net.sbo.mod.utils.game.World
import net.sbo.mod.utils.Helper

object MuteBuggedSpadeSounds {
    fun init() {
        // nothing for now, init() called for class loading to occur
    }

    /**
     * Adapted from https://github.com/hannibal002/SkyHanni/blob/b06eb22e41436d73538c89ab6fc05154e7ee2b80/src/main/java/at/hannibal2/skyhanni/features/event/diana/MuteBuggedSpade.kt#L14
     */
    @SboEvent
    fun onPlaySound(event: PlaySoundEvent) {
        if (World.getWorld() != "Hub" || !Helper.hasSpade) return

        val sound = event.sound
        val isRealMusic = sound.pitch == 1f && sound.volume == 1f && sound.x == 0.0 && sound.y == 0.0 && sound.z == 0.0

        if (sound.identifier.toString().replace("minecraft:", "").startsWith("music") && !isRealMusic) {
            event.isCanceled = true
        }
    }
}

