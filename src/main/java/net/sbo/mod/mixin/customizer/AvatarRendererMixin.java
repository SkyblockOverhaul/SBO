package net.sbo.mod.mixin.customizer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.sbo.mod.customizer.SboCustomizer;
import net.sbo.mod.utils.accessors.CustomizerRenderStateAccessor;
import org.jspecify.annotations.NonNull;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Customizer data for the name tag, the line itself is submitted in {@link EntityRendererMixin}.
 * <p>
 * order = 2000: other mods' injectors at the same spot apply first (default 1000), so these run last and see
 * the name tag and show-name decision other mods made, instead of being overwritten by them.
 * Nothing is cancelled or redirected, so other mods' injectors here keep working; require = 0 so a mod that
 * rebuilds these methods costs the customizer, not a crash.
 */
@Mixin(AvatarRenderer.class)
final class AvatarRendererMixin {
    /**
     * Customizer line, name size, name tag color and model size for the render state. Models in menus (inventory) get none
     * of them, those only belong in the world. The size only scales the model and where its name tag sits, the hitbox stays
     * as it is. Vanilla only sets the name tag attachment this frame while the name shows, otherwise it is last frame's and
     * already scaled.
     */
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"),
            order = 2000,
            require = 0
    )
    private final void sbo$extractCustomizer(@NonNull final Avatar entity, @NonNull final AvatarRenderState state, final float partialTick, @NonNull final CallbackInfo ci) {
        if (!(entity instanceof Player) || SboCustomizer.INSTANCE.getMenuModel()) {
            ((CustomizerRenderStateAccessor) state).sbo$setCustomizerLine(null);
            ((CustomizerRenderStateAccessor) state).sbo$setNameSize(1.0F);
            return;
        }
        final var uuid = entity.getUUID();
        ((CustomizerRenderStateAccessor) state).sbo$setCustomizerLine(SboCustomizer.INSTANCE.nameTagLine(uuid));
        ((CustomizerRenderStateAccessor) state).sbo$setNameSize(SboCustomizer.INSTANCE.nameSizeOf(uuid));
        if (state.nameTag != null) state.nameTag = SboCustomizer.INSTANCE.colorNameTag(uuid, state.nameTag);
        final var size = SboCustomizer.INSTANCE.sizeOf(uuid);
        if (size != 1.0F) {
            state.scale *= size;
            if (state.nameTag != null && state.nameTagAttachment != null) state.nameTagAttachment = state.nameTagAttachment.scale(size);
        }
    }

    /**
     * Vanilla never shows the camera's own name, in third person the own customizer should be visible. The own model in the
     * inventory keeps vanilla's choice, else F5 would put the name tag into the inventory. Comparing with mc.player by
     * reference is safe.
     */
    @ModifyReturnValue(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("RETURN"), order = 2000, require = 0)
    @SuppressWarnings("ReferenceEquality")
    private final boolean sbo$showOwnCustomizer(final boolean original, @NonNull final Avatar entity, final double distanceSq) {
        if (original || SboCustomizer.INSTANCE.getMenuModel()) return original;
        final var mc = Minecraft.getInstance();
        if (entity != mc.player || mc.options.getCameraType().isFirstPerson() || entity.isInvisible()) return false;

        return SboCustomizer.INSTANCE.showOwnNameTag();
    }
}
