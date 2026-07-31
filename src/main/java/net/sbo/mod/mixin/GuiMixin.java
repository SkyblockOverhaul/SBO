package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.render.RenderEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
final class GuiMixin {
    @Inject(method = "extractRenderState", at = @At(value = "RETURN"))
    private final void sbo$afterHudRender(@NonNull final DeltaTracker deltaTracker, final boolean shouldRenderLevel, final boolean resourcesLoaded, @NonNull final CallbackInfo ci, @Local @NonNull final GuiGraphicsExtractor graphics) {
        SBOEvent.INSTANCE.emit(new RenderEvent(graphics));
    }
}
