package net.sbo.mod.mixin.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Decorates the lines SkyblockAddons' compact tab list draws, see {@link SkyblockAddonsTabListParserMixin}.
 * Its lines stay § coded strings, so the decorated line only goes in where it is drawn; ordinal 1 is the middle lines,
 * 0 and 2 are header and footer. {@link SkyblockAddonsTabLineMixin} makes the column wide enough for it.
 * <p>
 * Exact signatures and require = 0: if SkyblockAddons changes them the mixin does not apply and nothing crashes.
 */
@Pseudo
@Mixin(targets = "com.fix3dll.skyblockaddons.features.tablist.TabListRenderer", remap = false)
final class SkyblockAddonsTabListRendererMixin {
    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyArg(
            method = "render(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getVisualOrder(Lnet/minecraft/network/chat/FormattedText;)Lnet/minecraft/util/FormattedCharSequence;", ordinal = 1),
            require = 0
    )
    private static @NonNull FormattedText sbo$decorateLine(@NonNull final FormattedText line) {
        final Component decorated = SboBadges.INSTANCE.decorateSbaTab(line.getString());
        return decorated != null ? decorated : line;
    }
}
