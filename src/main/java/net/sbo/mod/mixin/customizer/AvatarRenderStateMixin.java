package net.sbo.mod.mixin.customizer;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.network.chat.Component;
import net.sbo.mod.utils.accessors.CustomizerRenderStateAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
final class AvatarRenderStateMixin implements CustomizerRenderStateAccessor {
    @Unique
    private @Nullable Component sbo$customizerLine;
    @Unique
    private float sbo$nameSize = 1.0F;

    @Override
    public final @Nullable Component sbo$getCustomizerLine() {
        return sbo$customizerLine;
    }

    @Override
    public final void sbo$setCustomizerLine(@Nullable final Component line) {
        sbo$customizerLine = line;
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
