package net.sbo.mod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.guis.GuiKeyEvent;
import net.sbo.mod.utils.events.impl.guis.GuiRenderEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(@NonNull final GuiGraphics context, final int mouseX, final int mouseY, final float delta, @NonNull final CallbackInfo ci) {
        final Minecraft client = Minecraft.getInstance();
        final Screen screen = (Screen)(Object)this;
        SBOEvent.INSTANCE.emit(new GuiRenderEvent(client, screen, context, mouseX, mouseY, delta));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(final KeyEvent input, @NonNull final CallbackInfoReturnable<Boolean> cir) {
        final Minecraft client = Minecraft.getInstance();
        final Screen screen = (Screen)(Object)this;
        final int keyCode = input.key();
        SBOEvent.INSTANCE.emit(new GuiKeyEvent(client, screen, keyCode, cir));
    }
}
