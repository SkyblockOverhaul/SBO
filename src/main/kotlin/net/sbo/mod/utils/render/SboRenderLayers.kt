package net.sbo.mod.utils.render

import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object SboRenderLayers {
    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/beacon_beam_opaque_through_walls",
        RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_OPAQUE_THROUGH_WALLS)
            .withTexture("Sampler0", BeaconRenderer.BEAM_LOCATION)
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
