package net.sbo.mod.mixin.badges;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Font.class)
final class FontMixin {
    // Badge holders' names in any drawn text; only their colors change, so the width stays the same and needs no hook.
    // require = 0: no crash if it can't apply
    @ModifyVariable(
        method = "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private final @NonNull FormattedCharSequence sbo$recolorNames(@NonNull final FormattedCharSequence text) {
        final FormattedCharSequence recolored = SboBadges.INSTANCE.replaceEverywhere(text);
        return recolored != null ? recolored : text;
    }
}
