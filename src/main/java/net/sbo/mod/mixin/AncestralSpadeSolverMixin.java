package net.sbo.mod.mixin;

import net.sbo.mod.settings.categories.Diana;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;

/**
 * If you are a Firmament developer and have an issue with this mixin's existence do not hesitate to make a pull request.
 *
 * Reasoning: Users frequently report white boxed waypoints and a white line pointing to it under the ground typically when/after
 * using an Abiphone, and in other cases sometimes in the air as well. It seems Firmament does not verify that the guess is in a grass block,
 * which we do and so know that these waypoints are not from SBO, but users do not know this and frequently ask us.
 *
 * If the SBO's Arrow Guess is false, the original isEnabled makes Firmament behave normally.
 */
@Pseudo
@Mixin(targets = "moe.nea.firmament.features.diana.AncestralSpadeSolver", remap = false)
final class AncestralSpadeSolverMixin {
    @ModifyReturnValue(method = "isEnabled", at = @At("RETURN"))
    private final boolean sbo$disableAncestralSpadeSolverIfSBOEnabled(final boolean isEnabled) {
        return isEnabled && !Diana.INSTANCE.spadeGuess;
    }
}
