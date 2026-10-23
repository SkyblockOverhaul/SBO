package net.sbo.mod.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Skyblocker's fancy tab list reads the tab name straight from the PlayerInfo and skips
 * PlayerTabOverlay.getNameForDisplay, so the badge is added where its player entry takes the name.
 * The entry measures its width after that, so the logo fits.
 * <p>
 * No handler takes the PlayerElement itself: MixinExtras wants its exact type, which a @Pseudo mixin can't name.
 * require = 0: if Skyblocker changes the constructor the mixin does not apply.
 */
@Pseudo
@Mixin(targets = "de.hysky.skyblocker.skyblock.tabhud.widget.element.PlayerElement", remap = false)
final class SkyblockerPlayerElementMixin {
    // Player list: no name is passed, the second read of the tab name is the one the entry keeps
    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyExpressionValue(
            method = "<init>(Lnet/minecraft/client/multiplayer/PlayerInfo;Lnet/minecraft/network/chat/Component;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerInfo;getTabListDisplayName()Lnet/minecraft/network/chat/Component;", ordinal = 1),
            require = 0
    )
    private final @Nullable Component sbo$addBadge(@Nullable final Component name, @Local(argsOnly = true) @NonNull final PlayerInfo info) {
        return name != null ? SboBadges.INSTANCE.decorateTab(info, name) : null;
    }

    // Dungeon party list: passes the tab name without the class, changed before the entry first reads it
    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyVariable(
            method = "<init>(Lnet/minecraft/client/multiplayer/PlayerInfo;Lnet/minecraft/network/chat/Component;Z)V",
            at = @At(value = "LOAD", ordinal = 0),
            argsOnly = true,
            require = 0
    )
    private final @Nullable Component sbo$addBadgeToPassedName(@Nullable final Component name, @Local(argsOnly = true) @NonNull final PlayerInfo info) {
        return name != null && info.getTabListDisplayName() != null ? SboBadges.INSTANCE.decorateTab(info, name) : name;
    }
}
