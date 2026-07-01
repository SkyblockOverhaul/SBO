package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    @Inject(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void afterHudRender(@NonNull DeltaTracker tickCounter, boolean tick, boolean resourcesLoaded, @NonNull CallbackInfo ci, @Local @NonNull GuiGraphicsExtractor context) {
        SBOEvent.INSTANCE.emit(new RenderEvent(context));
    }
}
