package net.sbo.mod.mixin.events;

import net.minecraft.client.Minecraft;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.game.StartAttackingEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
final class StartAttackingMixin {
    @Shadow
    protected int missTime;

    @Inject(method = "startAttack", at = @At("HEAD"))
    private final void sbo$onStartAttack(@NonNull final CallbackInfoReturnable<Boolean> cir) {
        final Minecraft mc = (Minecraft) (Object) this;
        // Vanilla's early returns, clicks that do nothing don't emit
        if (this.missTime > 0 || mc.player == null || mc.level == null || mc.hitResult == null || mc.player.isHandsBusy()) return;

        SBOEvent.INSTANCE.emit(new StartAttackingEvent(mc.player, mc.hitResult, mc.level));
    }
}
