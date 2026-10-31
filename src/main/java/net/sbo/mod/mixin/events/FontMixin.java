package net.sbo.mod.mixin.events;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.sbo.mod.general.SboBadges;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
public class FontMixin {
    @ModifyVariable(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I",
                    at = @At("HEAD"), argsOnly = true)
    private FormattedCharSequence sbo$width(FormattedCharSequence text) {
        FormattedCharSequence r = SboBadges.INSTANCE.replaceEverywhere(text);
        return r != null ? r : text;
    }


    @ModifyVariable(
        method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At("HEAD"),
        argsOnly = true
    )
    private FormattedCharSequence sbo$draw(FormattedCharSequence text) {
        FormattedCharSequence r = SboBadges.INSTANCE.replaceEverywhere(text);
        return r != null ? r : text;
    }
}