package net.sbo.mod.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.sbo.mod.utils.input.KeybindConflicts;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A key in Minecraft's Key Binds is marked red like a double key when an SBO key has it too. */
@Mixin(targets = "net.minecraft.client.gui.screens.options.controls.KeyBindsList$KeyEntry")
abstract class KeyBindsListMixin {
    @Shadow
    @Final
    private KeyMapping key;

    @Shadow
    @Final
    private Button changeButton;

    @Shadow
    private boolean hasCollision;

    @Inject(method = "refreshEntry", at = @At("TAIL"))
    private void sbo$markSboKeys(@NonNull final CallbackInfo ci) {
        if (KeybindConflicts.markMinecraftKey(this.key, this.changeButton)) this.hasCollision = true;
    }
}
