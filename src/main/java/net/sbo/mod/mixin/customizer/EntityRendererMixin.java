package net.sbo.mod.mixin.customizer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.sbo.mod.customizer.SboCustomizer;
import net.sbo.mod.utils.accessors.CustomizerRenderStateAccessor;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Submits the customizer line above a player's name tag.
 * <p>
 * Here and not in AvatarRenderer.submitNameDisplay: name tag mods cancel or replace that method to draw their own
 * name tags, which would take the customizer with it. This runs for every entity before the name tag is drawn, with the
 * same pose, so the customizer stays as long as the player is rendered and has a name tag.
 */
@Mixin(EntityRenderer.class)
final class EntityRendererMixin {
    @Unique
    private static final float sbo$LINE_HEIGHT = 9.0F * 1.15F * 0.025F;
    @Unique
    private static final float sbo$TEXT_HEIGHT = 9.0F * 0.025F;

    /** The customizer line sits on top of the name line, which grows or shrinks upward with the name size. */
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            require = 0
    )
    private final void sbo$submitCustomizer(@NonNull final EntityRenderState renderState, @NonNull final PoseStack poseStack, @NonNull final SubmitNodeCollector collector, @NonNull final CameraRenderState camera, @NonNull final CallbackInfo ci) {
        if (!(renderState instanceof final AvatarRenderState state)) return;
        final var line = ((CustomizerRenderStateAccessor) state).sbo$getCustomizerLine();
        if (line == null || state.nameTag == null || state.nameTagAttachment == null) return;

        poseStack.pushPose();
        final var base = state.scoreText != null ? sbo$LINE_HEIGHT * 2.0F : sbo$LINE_HEIGHT;
        poseStack.translate(0.0F, base + sbo$TEXT_HEIGHT * (((CustomizerRenderStateAccessor) state).sbo$getNameSize() - 1.0F), 0.0F);
        sbo$scaleText(poseStack, state.nameTagAttachment, SboCustomizer.INSTANCE.lineSize());
        final var offset = state.showExtraEars ? -10 : 0;
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
        final var size = sbo$nameSize(state);
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
    private static final float sbo$nameSize(@NonNull final EntityRenderState state) {
        if (!(state instanceof final AvatarRenderState avatar) || state.nameTag == null || state.nameTagAttachment == null) return 1.0F;
        return ((CustomizerRenderStateAccessor) avatar).sbo$getNameSize();
    }

    /**
     * The name tag renderer moves to the attachment (+0.5 up) first, then turns to the camera and shrinks to text size.
     * Scaling around that anchor sizes only the text; the lift keeps the line's bottom where it was.
     */
    @Unique
    private static final void sbo$scaleText(@NonNull final PoseStack poseStack, @NonNull final Vec3 attachment, final float size) {
        if (size == 1.0F) return;
        final var ax = (float) attachment.x;
        final var ay = (float) attachment.y + 0.5F;
        final var az = (float) attachment.z;
        poseStack.translate(ax, ay + sbo$TEXT_HEIGHT * (size - 1.0F), az);
        poseStack.scale(size, size, size);
        poseStack.translate(-ax, -ay, -az);
    }
}
