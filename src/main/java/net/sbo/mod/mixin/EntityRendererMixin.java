package net.sbo.mod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.sbo.mod.utils.accessors.BadgeRenderStateAccessor;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Submits the badge line above a player's name tag.
 * <p>
 * Here and not in AvatarRenderer.submitNameDisplay: name tag mods cancel or replace that method to draw their own
 * name tags, which would take the badge with it. This runs for every entity before the name tag is drawn, with the
 * same pose, so the badge stays as long as the player is rendered and has a name tag.
 */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @Unique
    private static final float sbo$LINE_HEIGHT = 9.0F * 1.15F * 0.025F;

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            require = 0
    )
    private final void sbo$submitBadge(@NonNull final EntityRenderState renderState, @NonNull final PoseStack poseStack, @NonNull final SubmitNodeCollector collector, @NonNull final CameraRenderState camera, @NonNull final CallbackInfo ci) {
        if (!(renderState instanceof final AvatarRenderState state)) return;
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
