package net.sbo.mod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.guis.GuiKeyEvent;
import net.sbo.mod.utils.events.impl.guis.GuiRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void onRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = (Screen)(Object)this;
        SBOEvent.INSTANCE.emit(new GuiRenderEvent(client, screen, context, mouseX, mouseY, delta));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        Minecraft client = Minecraft.getInstance();
        Screen screen = (Screen)(Object)this;
        int keyCode = input.key();
        SBOEvent.INSTANCE.emit(new GuiKeyEvent(client, screen, keyCode, cir));
    }
}
