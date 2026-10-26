package net.sbo.mod.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.StartAttackingEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class StartAttackingMixin {

    @Inject(method = "startAttack", at = @At("HEAD"))
    private void onLeftClick(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft) (Object) this;
        if (mc == null) return;

        HitResult hit = mc.hitResult;

        SBOEvent.INSTANCE.emit(new StartAttackingEvent(mc.player, hit, mc.level));
    }
}