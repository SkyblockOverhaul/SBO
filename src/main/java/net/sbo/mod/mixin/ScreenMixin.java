package net.sbo.mod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.KeyInput;
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
    public void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = (Screen)(Object)this;
        SBOEvent.INSTANCE.emit(new GuiRenderEvent(client, screen, context, mouseX, mouseY, delta));
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = (Screen)(Object)this;
        int keyCode = input.key();
        SBOEvent.INSTANCE.emit(new GuiKeyEvent(client, screen, keyCode, cir));
    }
}
