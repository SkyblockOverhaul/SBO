package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.network.chat.Component;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.List;

/**
 * SkyHanni's compact tab list rebuilds player lines from § codes, which loses badge hex colors and the logo font,
 * and its name parsing takes the team tag for the Hypixel rank. So it reads the lines undecorated, and
 * {@link SkyHanniAdvancedPlayerListMixin} decorates the lines it draws instead.
 */
@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.utils.TabListData", remap = false)
final class SkyHanniTabListDataMixin {
    @SuppressWarnings("UnresolvedMixinReference")
    @WrapMethod(method = "readTabList()Ljava/util/List;", require = 0)
    private final @Nullable List<Component> sbo$readUndecorated(@NonNull final Operation<List<Component>> original) {
        return SboBadges.INSTANCE.undecoratedTab(original::call);
    }
}
