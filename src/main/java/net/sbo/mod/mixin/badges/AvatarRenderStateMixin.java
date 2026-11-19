package net.sbo.mod.mixin.badges;

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
    @Unique
    private float sbo$nameSize = 1.0F;

    @Override
    public final @Nullable Component sbo$getBadgeLine() {
        return sbo$badgeLine;
    }

    @Override
    public final void sbo$setBadgeLine(@Nullable final Component line) {
        sbo$badgeLine = line;
    }

    @Override
    public final float sbo$getNameSize() {
        return sbo$nameSize;
    }

    @Override
    public final void sbo$setNameSize(final float size) {
        sbo$nameSize = size;
    }
}
