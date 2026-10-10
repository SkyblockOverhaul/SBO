package net.sbo.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;
import net.sbo.mod.general.SboBadges;
import net.sbo.mod.utils.accessors.BadgeRenderStateAccessor;
import org.jspecify.annotations.NonNull;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Unique
    private static final float sbo$LINE_HEIGHT = 9.0F * 1.15F * 0.025F;

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
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

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private final void sbo$submitBadge(@NonNull final AvatarRenderState state, @NonNull final PoseStack poseStack, @NonNull final SubmitNodeCollector collector, @NonNull final CameraRenderState camera, @NonNull final CallbackInfo ci) {
        final Component line = ((BadgeRenderStateAccessor) state).sbo$getBadgeLine();
        if (line == null || state.nameTag == null || state.nameTagAttachment == null) return;

        poseStack.pushPose();
        poseStack.translate(0.0F, state.scoreText != null ? sbo$LINE_HEIGHT * 2.0F : sbo$LINE_HEIGHT, 0.0F);
        final int offset = state.showExtraEars ? -10 : 0;
        //#if MC > 26.1
        //$$ collector.submitNameTag(poseStack, state.nameTagAttachment, offset, line, !state.isDiscrete, state.lightCoords, camera);
        //#else
        collector.submitNameTag(poseStack, state.nameTagAttachment, offset, line, !state.isDiscrete, state.lightCoords, state.distanceToCameraSq, camera);
        //#endif
        poseStack.popPose();
    }
}
