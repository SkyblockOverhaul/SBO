package net.sbo.mod.utils.render

import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import java.util.OptionalDouble
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.LayeringTransform

object SboRenderLayers {
    @JvmField
    val FILLED_BOX: RenderType = RenderType.create(
        "sbo/filled_box",
        RenderSetup.builder(RenderPipelines.DEBUG_FILLED_BOX)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .sortOnUpload()
            .createRenderSetup()
    )

    @JvmField
    val FILLED_BOX_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/filled_box_through_walls",
        RenderSetup.builder(SboRenderPipelines.FILLED_BOX_THROUGH_WALLS)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .sortOnUpload()
            .createRenderSetup()
    )

    @JvmField
    val LINES: RenderType = RenderType.create(
        "sbo/lines",
        RenderSetup.builder(SboRenderPipelines.LINES)
            .sortOnUpload()
            .createRenderSetup()
    )

    @JvmField
    val LINES_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/lines_through_walls",
        RenderSetup.builder(SboRenderPipelines.LINES_THROUGH_WALLS)
            .sortOnUpload()
            .createRenderSetup()
    )

    val BEACON_BEAM_OPAQUE: RenderType = RenderType.create(
        "sbo/beacon_beam_opaque",
        RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_OPAQUE)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .createRenderSetup()
    )

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/beacon_beam_opaque_through_walls",
        RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_OPAQUE_THROUGH_WALLS)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .createRenderSetup()
    )

    val BEACON_BEAM_TRANSLUCENT: RenderType = RenderType.create(
        "sbo/beacon_beam_translucent",
        RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_TRANSLUCENT)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .sortOnUpload()
            .createRenderSetup()
    )

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/beacon_beam_translucent_esp",
        RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
            .sortOnUpload()
            .createRenderSetup()
    )
}
