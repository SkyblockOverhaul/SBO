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
public class EntityMixin implements EntityAccessor {
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
    public void getTeamColorValue(@NonNull final CallbackInfoReturnable<Integer> cir) {
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
    public void isGlowing(@NonNull final CallbackInfoReturnable<Boolean> cir) {
        if (sbo$hasCustomGlow()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void sbo$setGlowing(final boolean glowing) {
        sbo$glowing = glowing;
    }

    @Override
    public void sbo$setGlowingColor(final int color) {
        this.sbo$glowingColor = color;
    }

    @Override
    public void sbo$glowTime(final long time) {
        this.sbo$glowTime = System.currentTimeMillis() + time;
        this.sbo$glowing = false;
    }

    @Override
    public void sbo$setGlowingThisFrame(final boolean glowing) {
        this.sbo$glowingThisFrame = glowing;
    }

    @Unique
    private boolean sbo$hasCustomGlow() {
        if (this.sbo$glowingThisFrame) return true;
        if (this.sbo$glowTime > System.currentTimeMillis()) return true;

        // Reset time if expired
        if (this.sbo$glowTime != -1 && this.sbo$glowTime <= System.currentTimeMillis()) {
            sbo$glowTime = -1L;
        }

        return this.sbo$glowing;
    }
}
