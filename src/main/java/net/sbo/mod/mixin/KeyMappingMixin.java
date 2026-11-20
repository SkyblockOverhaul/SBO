package net.sbo.mod.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.sbo.mod.utils.input.KeyInput;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every key and mouse button press/release outside of screens, for the SBO keys bound to a mouse button. */
@Mixin(KeyMapping.class)
final class KeyMappingMixin {
    @Inject(method = "set", at = @At("HEAD"))
    private static final void sbo$trackKey(final InputConstants.@NonNull Key key, final boolean down, @NonNull final CallbackInfo ci) {
        KeyInput.onKeyMappingSet(key, down);
    }
}
