package net.sbo.mod.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.sbo.mod.utils.accessors.TabListAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerTabOverlay.class)
final class PlayerTabOverlayVisibleMixin implements TabListAccessor {
    @Shadow
    private boolean visible;

    @Override
    public final boolean sbo$isVisible() {
        return visible;
    }
}
