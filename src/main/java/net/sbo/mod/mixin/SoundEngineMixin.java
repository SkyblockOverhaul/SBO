package net.sbo.mod.mixin;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.PlaySoundEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
final class SoundEngineMixin {
    /**
     * Adapted from <a href="https://github.com/hannibal002/SkyHanni/blob/b06eb22e41436d73538c89ab6fc05154e7ee2b80/src/main/java/at/hannibal2/skyhanni/mixins/transformers/MixinSoundEngine.java#L27">...</a>
     */
    @Inject(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"), cancellable = true)
    private final void sbo$onPlaySound(@NonNull final SoundInstance sound, @NonNull final CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        final var event = new PlaySoundEvent(sound, false);
        SBOEvent.INSTANCE.emit(event);

        if (event.isCanceled()) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }
}
