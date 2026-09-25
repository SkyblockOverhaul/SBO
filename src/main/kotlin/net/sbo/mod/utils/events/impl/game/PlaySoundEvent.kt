package net.sbo.mod.utils.events.impl.game

import net.minecraft.client.resources.sounds.SoundInstance

/**
 * Called when a sound is about to be played.
 *
 * @param sound The sound that is going to play unless canceled.
 * @param isCanceled Whether the event is canceled. Can be modified by event listeners.
 */
class PlaySoundEvent(
    val sound: SoundInstance,
    var isCanceled: Boolean = false
)
