package net.sbo.mod.mixin;

import net.minecraft.world.entity.Entity;
import net.sbo.mod.utils.accessors.EntityAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements EntityAccessor {
    @Unique
    private boolean sbo$glowing = false;
    @Unique
    private int sbo$glowingColor = 0xFFFFFF;
    @Unique
    private long sbo$glowTime = -1;
    @Unique
    private boolean sbo$glowingThisFrame = false;

    @Inject(
            method = "getTeamColor",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getTeamColorValue(CallbackInfoReturnable<Integer> cir) {
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
    public void isGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (sbo$hasCustomGlow()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public void sbo$setGlowing(boolean glowing) {
        this.sbo$glowing = glowing;
    }

    @Override
    public void sbo$setGlowingColor(int color) {
        this.sbo$glowingColor = color;
    }

    @Override
    public void sbo$glowTime(long time) {
        this.sbo$glowTime = System.currentTimeMillis() + time;
        this.sbo$glowing = false;
    }

    @Override
    public void sbo$setGlowingThisFrame(boolean glowing) {
        this.sbo$glowingThisFrame = glowing;
    }

    @Unique
    private boolean sbo$hasCustomGlow() {
        if (this.sbo$glowingThisFrame) return true;
        if (this.sbo$glowTime > System.currentTimeMillis()) return true;

        // Reset time if expired
        if (this.sbo$glowTime != -1 && this.sbo$glowTime <= System.currentTimeMillis()) {
            this.sbo$glowTime = -1;
        }

        return this.sbo$glowing;
    }
}