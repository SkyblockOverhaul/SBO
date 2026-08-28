package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.sbo.mod.settings.categories.Diana;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * If you are a Skyblocker developer and have an issue with this mixin's existence do not hesitate to make a pull request.
 *
 * Reasoning: Users report Skyblocker persisting lots of orange burrow waypoints even after fully digging them and they often
 * blame SBO until we convince them that it's Skyblocker that they need to disable. This ensures that Skyblocker won't get in the way
 * if the user choose to use the SBO's Arrow Guess option which is even more powerful than Skyblocker's implementation.
 *
 * If the SBO's Arrow Guess is false, the original isActive makes Skyblocker behave normally.
 */
@Pseudo
@Mixin(targets = "de.hysky.skyblocker.skyblock.waypoint.MythologicalRitual", remap = false)
final class MythologicalRitualMixin {
    @ModifyReturnValue(method = "isActive", at = @At("RETURN"))
    private static final boolean sbo$disableSkyblockerMythologicalRitualHelperIfSBOEnabled(final boolean isActive) {
        return isActive && !Diana.INSTANCE.getArrowGuess();
    }
}
