package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.sbo.mod.utils.input.SboKeyBinds;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The SBO keys used to be in Minecraft's Key Binds, a button next to Reset Keys and Done opens them in the SBO settings now. */
@Mixin(KeyBindsScreen.class)
final class KeyBindsScreenMixin {
    @Unique
    private @Nullable LinearLayout sbo$footer;

    @ModifyExpressionValue(method = "addFooter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private final @NonNull LayoutElement sbo$rememberFooter(@NonNull final LayoutElement footer) {
        if (footer instanceof LinearLayout row) this.sbo$footer = row;
        return footer;
    }

    // After Reset Keys and Done are in the row
    @Inject(method = "addFooter", at = @At("TAIL"))
    private final void sbo$addKeybindsButton(@NonNull final CallbackInfo ci) {
        if (this.sbo$footer == null) return;
        this.sbo$footer.addChild(SboKeyBinds.controlsButton((Screen) (Object) this));
        this.sbo$footer = null;
    }
}
