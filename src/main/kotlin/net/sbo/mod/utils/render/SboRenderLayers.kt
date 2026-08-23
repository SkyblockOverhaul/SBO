package net.sbo.mod.utils.render

import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object SboRenderLayers {
    val LINES_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/lines_through_walls",
        RenderSetup.builder(SboRenderPipelines.LINES_THROUGH_WALLS)
            .createRenderSetup()
    )
}
