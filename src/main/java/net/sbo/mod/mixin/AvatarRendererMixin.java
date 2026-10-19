package net.sbo.mod.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.sbo.mod.general.SboBadges;
import net.sbo.mod.utils.accessors.BadgeRenderStateAccessor;
import org.jspecify.annotations.NonNull;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Badge data for the name tag, the line itself is submitted in {@link EntityRendererMixin}.
 * <p>
 * order = 2000: other mods' injectors at the same spot apply first (default 1000), so these run last and see
 * the name tag and show-name decision other mods made, instead of being overwritten by them.
 * Nothing is cancelled or redirected, so other mods' injectors here keep working; require = 0 so a mod that
 * rebuilds these methods costs the badge, not a crash.
 */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"),
            order = 2000,
            require = 0
    )
    private final void sbo$extractBadge(@NonNull final Avatar entity, @NonNull final AvatarRenderState state, final float partialTick, @NonNull final CallbackInfo ci) {
        if (!(entity instanceof Player)) {
            ((BadgeRenderStateAccessor) state).sbo$setBadgeLine(null);
            return;
        }
        final UUID uuid = entity.getUUID();
        ((BadgeRenderStateAccessor) state).sbo$setBadgeLine(SboBadges.INSTANCE.nameTagLine(uuid));
        if (state.nameTag != null) state.nameTag = SboBadges.INSTANCE.colorNameTag(uuid, state.nameTag);
    }

    // Vanilla never shows the camera's own name, in third person the own badge should be visible
    @ModifyReturnValue(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("RETURN"), order = 2000, require = 0)
    private final boolean sbo$showOwnBadge(final boolean original, @NonNull final Avatar entity, final double distanceSq) {
        if (original) return true;
        final Minecraft mc = Minecraft.getInstance();
        if (entity != mc.player || mc.options.getCameraType().isFirstPerson() || entity.isInvisible()) return false;
        return SboBadges.INSTANCE.showOwnNameTag();
    }
}
