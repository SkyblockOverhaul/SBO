package net.sbo.mod.mixin;

import net.minecraft.world.entity.Entity;
import net.sbo.mod.utils.accessors.EntityAccessor;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
final class EntityMixin implements EntityAccessor {
    @Unique
    private boolean sbo$glowing;
    @Unique
    private int sbo$glowingColor = 0xFFFFFF;
    @Unique
    private long sbo$glowTime = -1L;
    @Unique
    private boolean sbo$glowingThisFrame;

    @Inject(
            method = "getTeamColor",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void sbo$getTeamColorValue(@NonNull final CallbackInfoReturnable<Integer> cir) {
        if (sbo$hasCustomGlow()) {
            cir.setReturnValue(sbo$glowingColor);
            // We don't reset frame glow here to ensure it persists through the render cycle
        }
    }

    @Inject(
            method = "isCurrentlyGlowing",
            at = @At("HEAD"),
            cancellable = true
    )
    private final void sbo$isGlowing(@NonNull final CallbackInfoReturnable<Boolean> cir) {
        if (sbo$hasCustomGlow()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public final void sbo$setGlowing(final boolean glowing) {
        sbo$glowing = glowing;
    }

    @Override
    public final void sbo$setGlowingColor(final int color) {
        sbo$glowingColor = color;
    }

    @Override
    public final void sbo$glowTime(final long time) {
        sbo$glowTime = System.nanoTime() + time;
        sbo$glowing = false;
    }

    @Override
    public final void sbo$setGlowingThisFrame(final boolean glowing) {
        sbo$glowingThisFrame = glowing;
    }

    @Unique
    private final boolean sbo$hasCustomGlow() {
        if (sbo$glowingThisFrame) return true;
        if (sbo$glowTime > System.nanoTime()) return true;

        // Reset time if expired
        if (sbo$glowTime != -1L && sbo$glowTime <= System.nanoTime()) {
            sbo$glowTime = -1L;
        }

        return sbo$glowing;
    }
}
