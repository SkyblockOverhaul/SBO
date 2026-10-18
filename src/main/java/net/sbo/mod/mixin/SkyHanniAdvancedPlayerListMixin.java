package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.Component;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Decorates the lines SkyHanni's compact tab list draws, see {@link SkyHanniTabListDataMixin}.
 * The decorated line goes in as the TabLine's custom name: SkyHanni draws it and measures the column with it,
 * while the plain line stays for its own name and skin lookups.
 * <p>
 * Exact signatures and require = 0: if SkyHanni changes them the mixin does not apply and nothing crashes.
 */
@Pseudo
@Mixin(targets = "at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList", remap = false)
final class SkyHanniAdvancedPlayerListMixin {
    @Unique
    private static final int sbo$CUSTOM_NAME_DEFAULT = 4;

    // Players SkyHanni rebuilt: their custom name
    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyArg(
            method = "createTabLine(Lnet/minecraft/network/chat/Component;Lat/hannibal2/skyhanni/features/misc/compacttablist/TabStringType;)Lat/hannibal2/skyhanni/features/misc/compacttablist/TabLine;",
            at = @At(value = "INVOKE", target = "Lat/hannibal2/skyhanni/features/misc/compacttablist/TabLine;<init>(Lnet/minecraft/network/chat/Component;Lat/hannibal2/skyhanni/features/misc/compacttablist/TabStringType;Lnet/minecraft/network/chat/Component;)V"),
            index = 2,
            require = 0
    )
    private final @Nullable Component sbo$decorateCustomName(@Nullable final Component customName) {
        final Component decorated = SboBadges.INSTANCE.decorateSkyHanniTab(customName);
        return decorated != null ? decorated : customName;
    }

    // Lines SkyHanni shows as they are (dungeons, Kuudra, lines it does not parse): the decorated line becomes the custom name
    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyArg(
            method = "createTabLine(Lnet/minecraft/network/chat/Component;Lat/hannibal2/skyhanni/features/misc/compacttablist/TabStringType;)Lat/hannibal2/skyhanni/features/misc/compacttablist/TabLine;",
            at = @At(value = "INVOKE", target = "Lat/hannibal2/skyhanni/features/misc/compacttablist/TabLine;<init>(Lnet/minecraft/network/chat/Component;Lat/hannibal2/skyhanni/features/misc/compacttablist/TabStringType;Lnet/minecraft/network/chat/Component;ILkotlin/jvm/internal/DefaultConstructorMarker;)V"),
            index = 2,
            require = 0
    )
    private final @Nullable Component sbo$decorateLine(@Nullable final Component customName, @Local(argsOnly = true) @NonNull final Component component) {
        return SboBadges.INSTANCE.decorateSkyHanniTab(component);
    }

    // Kotlin's default arguments: without clearing the bit the constructor would set the custom name back to null
    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyArg(
            method = "createTabLine(Lnet/minecraft/network/chat/Component;Lat/hannibal2/skyhanni/features/misc/compacttablist/TabStringType;)Lat/hannibal2/skyhanni/features/misc/compacttablist/TabLine;",
            at = @At(value = "INVOKE", target = "Lat/hannibal2/skyhanni/features/misc/compacttablist/TabLine;<init>(Lnet/minecraft/network/chat/Component;Lat/hannibal2/skyhanni/features/misc/compacttablist/TabStringType;Lnet/minecraft/network/chat/Component;ILkotlin/jvm/internal/DefaultConstructorMarker;)V"),
            index = 3,
            require = 0
    )
    private final int sbo$keepCustomName(final int defaults) {
        return defaults & ~sbo$CUSTOM_NAME_DEFAULT;
    }
}
