package net.sbo.mod.mixin;

import net.minecraft.core.BlockPos;
import net.sbo.mod.settings.categories.Diana;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * If you are a Firmament developer and have an issue with this mixin's existence do not hesitate to make a pull request.
 * <p>
 * Reasoning: Users frequently report white boxed waypoints and a white line pointing to it under the ground typically when/after
 * using an Abiphone, and in other cases sometimes in the air as well. It seems Firmament does not verify that the guess is in a grass block,
 * which we do and so know that these waypoints are not from SBO, but users do not know this and frequently ask us.
 * <p>
 * If the SBO's Arrow Guess is false, the original isEnabled makes Firmament behave normally.
 */
@Pseudo
@Mixin(targets = "moe.nea.firmament.features.diana.NearbyBurrowsSolver", remap = false)
final class NearbyBurrowsSolverMixin {
    // no isEnabled() in this class, accesses config field directly, kind of hacky way to ci.cancel them but whatever
    @Inject(method = "onParticles", at = @At("HEAD"), cancellable = true)
    private final void sbo$disableNearbyBurrowsSolverOnParticlesIfSBOEnabled(@NonNull @Coerce final Object event, @NonNull final CallbackInfo ci) {
        if (Diana.INSTANCE.getCloseBurrowDetection()) {
            ci.cancel();
        }
    }

    @Inject(method = "onRender", at = @At("HEAD"), cancellable = true)
    private final void sbo$disableNearbyBurrowsSolverOnRenderIfSBOEnabled(@NonNull @Coerce final Object event, @NonNull final CallbackInfo ci) {
        if (Diana.INSTANCE.getCloseBurrowDetection()) {
            ci.cancel();
        }
    }

    @Inject(method = "onBlockClick", at = @At("HEAD"), cancellable = true)
    private final void sbo$disableNearbyBurrowsSolverOnBlockClickIfSBOEnabled(@NonNull final BlockPos blockPos, @NonNull final CallbackInfo ci) {
        if (Diana.INSTANCE.getCloseBurrowDetection()) {
            ci.cancel();
        }
    }
}
