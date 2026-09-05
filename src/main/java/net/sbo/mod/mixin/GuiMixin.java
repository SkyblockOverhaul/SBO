package net.sbo.mod.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.render.RenderEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#if MC > 26.1
//$$ import com.llamalad7.mixinextras.sugar.Local;
//$$ import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent;
//$$ import org.jspecify.annotations.Nullable;
//#endif

@Mixin(Gui.class)
final class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    //#if MC > 26.1
    //$$ private final void sbo$afterHudRender(@NonNull final DeltaTracker deltaTracker, final boolean shouldRenderLevel, final boolean resourcesLoaded, @NonNull final CallbackInfo ci, @Local @NonNull final GuiGraphicsExtractor graphics) {
    //#else
    private final void sbo$afterHudRender(@NonNull final GuiGraphicsExtractor graphics, @NonNull final DeltaTracker deltaTracker, @NonNull final CallbackInfo ci) {
    //#endif
        SBOEvent.INSTANCE.emit(new RenderEvent(graphics));
    }

    //#if MC > 26.1
    //$$ @Inject(method = "setScreen", at = @At("HEAD"))
    //$$ private final void sbo$onSetScreen(@Nullable final Screen screen, @NonNull final CallbackInfo ci) {
    //$$    if (screen != null) {
    //$$        SBOEvent.INSTANCE.emit(new GuiOpenEvent(screen, ci));
    //$$    }
    //$$ }
    //#endif
}
