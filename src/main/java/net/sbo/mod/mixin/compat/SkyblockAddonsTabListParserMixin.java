package net.sbo.mod.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * SkyblockAddons' compact tab list turns the tab names into § coded strings, which loses badge hex colors and the
 * logo font, and its username pattern takes the team tag for the Hypixel rank. So it reads the lines undecorated, and
 * {@link SkyblockAddonsTabListRendererMixin} and {@link SkyblockAddonsTabLineMixin} decorate the lines it draws instead.
 */
@Pseudo
@Mixin(targets = "com.fix3dll.skyblockaddons.features.tablist.TabListParser", remap = false)
final class SkyblockAddonsTabListParserMixin {
    @SuppressWarnings("UnresolvedMixinReference")
    @WrapMethod(method = "parse()V", require = 0)
    private static void sbo$parseUndecorated(@NonNull final Operation<Void> original) {
        SboBadges.INSTANCE.undecoratedTab(original::call);
    }
}
