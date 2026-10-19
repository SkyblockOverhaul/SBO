package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.sbo.mod.general.SboBadges;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerTabOverlay.class)
final class PlayerTabOverlayMixin {
    // order = 2000: runs after other mods changed the line, so they don't drop the badge; require = 0: no crash if it can't apply
    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"), order = 2000, require = 0)
    private final @NonNull Component sbo$addBadge(@NonNull final Component original, @NonNull final PlayerInfo info) {
        return SboBadges.INSTANCE.decorateTab(info, original);
    }
}
