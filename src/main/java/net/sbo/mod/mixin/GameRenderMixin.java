package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.render.RenderEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRenderMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void afterHudRender(@NonNull final DeltaTracker tickCounter, final boolean tick, @NonNull final CallbackInfo ci, @Local @NonNull final GuiGraphics context) {
        SBOEvent.INSTANCE.emit(new RenderEvent(context));
    }
}
