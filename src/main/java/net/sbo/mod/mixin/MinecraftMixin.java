package net.sbo.mod.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.sbo.mod.utils.events.SBOEvent;
import net.sbo.mod.utils.events.impl.guis.GuiOpenEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"))
    public void onSetScreen(@Nullable final Screen screen, @NonNull final CallbackInfo ci) {
        if (screen != null) {
            SBOEvent.INSTANCE.emit(new GuiOpenEvent(screen, ci));
        }
    }
}