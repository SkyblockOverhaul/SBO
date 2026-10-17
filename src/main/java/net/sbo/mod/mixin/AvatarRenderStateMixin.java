package net.sbo.mod.mixin;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.sbo.mod.utils.accessors.BadgeRenderStateAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
final class AvatarRenderStateMixin implements BadgeRenderStateAccessor {
    @Unique
    private @Nullable Component sbo$badgeLine;

    @Override
    public final @Nullable Component sbo$getBadgeLine() {
        return sbo$badgeLine;
    }

    @Override
    public final void sbo$setBadgeLine(@Nullable final Component line) {
        sbo$badgeLine = line;
    }
}
