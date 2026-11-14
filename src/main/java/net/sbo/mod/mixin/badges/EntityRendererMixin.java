package net.sbo.mod.mixin.badges;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.sbo.mod.general.SboBadges;
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
    @Unique
    private static final float sbo$TEXT_HEIGHT = 9.0F * 0.025F;

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
        // Sits on top of the name line, which grows or shrinks upward with the name size
        final float base = state.scoreText != null ? sbo$LINE_HEIGHT * 2.0F : sbo$LINE_HEIGHT;
        poseStack.translate(0.0F, base + sbo$TEXT_HEIGHT * (((BadgeRenderStateAccessor) state).sbo$getNameSize() - 1.0F), 0.0F);
        sbo$scaleText(poseStack, state.nameTagAttachment, SboBadges.INSTANCE.lineSize());
        final int offset = state.showExtraEars ? -10 : 0;
        //#if MC > 26.1
        //$$ collector.submitNameTag(poseStack, state.nameTagAttachment, offset, line, !state.isDiscrete, state.lightCoords, camera);
        //#else
        collector.submitNameTag(poseStack, state.nameTagAttachment, offset, line, !state.isDiscrete, state.lightCoords, state.distanceToCameraSq, camera);
        //#endif
        poseStack.popPose();
    }

    @Unique
    private static final String sbo$NAME_DISPLAY = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V";

    /**
     * A player's name line in the viewer's name size. The second submitNameTag call is the name, the first the score
     * line under it, which stays as it is. Matched by name only, so it fits every version's parameters.
     */
    @Inject(method = sbo$NAME_DISPLAY, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag", ordinal = 1), require = 0)
    private final void sbo$sizeName(@NonNull final EntityRenderState state, @NonNull final PoseStack poseStack, @NonNull final SubmitNodeCollector collector, @NonNull final CameraRenderState camera, final int offset, @NonNull final CallbackInfo ci) {
        final float size = sbo$nameSize(state);
        if (size == 1.0F) return;
        poseStack.pushPose();
        sbo$scaleText(poseStack, state.nameTagAttachment, size);
    }

    @Inject(method = sbo$NAME_DISPLAY, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag", ordinal = 1, shift = At.Shift.AFTER), require = 0)
    private final void sbo$sizeNameEnd(@NonNull final EntityRenderState state, @NonNull final PoseStack poseStack, @NonNull final SubmitNodeCollector collector, @NonNull final CameraRenderState camera, final int offset, @NonNull final CallbackInfo ci) {
        if (sbo$nameSize(state) != 1.0F) poseStack.popPose();
    }

    /** Name size for a player's name line, 1 for other entities, players without SBO looks or without a name to draw. */
    @Unique
    private static float sbo$nameSize(@NonNull final EntityRenderState state) {
        if (!(state instanceof final AvatarRenderState avatar) || state.nameTag == null || state.nameTagAttachment == null) return 1.0F;
        return ((BadgeRenderStateAccessor) avatar).sbo$getNameSize();
    }

    /**
     * The name tag renderer moves to the attachment (+0.5 up) first, then turns to the camera and shrinks to text size.
     * Scaling around that anchor sizes only the text; the lift keeps the line's bottom where it was.
     */
    @Unique
    private static void sbo$scaleText(@NonNull final PoseStack poseStack, @NonNull final Vec3 attachment, final float size) {
        if (size == 1.0F) return;
        final float ax = (float) attachment.x, ay = (float) attachment.y + 0.5F, az = (float) attachment.z;
        poseStack.translate(ax, ay + sbo$TEXT_HEIGHT * (size - 1.0F), az);
        poseStack.scale(size, size, size);
        poseStack.translate(-ax, -ay, -az);
    }
}
