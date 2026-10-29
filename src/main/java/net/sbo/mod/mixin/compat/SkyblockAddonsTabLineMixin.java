package net.sbo.mod.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * SkyblockAddons measures its columns with the plain line, the decorated one {@link SkyblockAddonsTabListRendererMixin}
 * draws is measured instead, so the logo fits.
 * require = 0: if SkyblockAddons changes the method the mixin does not apply.
 */
@Pseudo
@Mixin(targets = "com.fix3dll.skyblockaddons.features.tablist.TabLine", remap = false)
final class SkyblockAddonsTabLineMixin {
    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
            method = "getWidth()I",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I"),
            require = 0
    )
    private final int sbo$measureDecorated(@NonNull final Font font, @NonNull final String line, @NonNull final Operation<Integer> original) {
        final Component decorated = SboBadges.INSTANCE.decorateSbaTab(line);
        return decorated != null ? font.width(decorated) : original.call(font, line);
    }
}
