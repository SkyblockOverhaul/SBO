package net.sbo.mod.utils.render

import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer
import net.minecraft.util.TriState
import java.util.OptionalDouble
//#if MC > 1.21.10
//$$ import net.minecraft.client.render.RenderSetup
//$$ import net.minecraft.client.render.LayeringTransform
//#else
import net.minecraft.client.render.RenderPhase
//#endif

object SboRenderLayers {
    @JvmField
    val FILLED_BOX: RenderLayer = RenderLayer.of(
        "sbo/filled_box",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(RenderPipelines.DEBUG_FILLED_BOX)
        //$$     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        RenderPipelines.DEBUG_FILLED_BOX,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
        //#endif
    )

    @JvmField
    val FILLED_BOX_THROUGH_WALLS: RenderLayer = RenderLayer.of(
        "sbo/filled_box_through_walls",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.FILLED_BOX_THROUGH_WALLS)
        //$$     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SboRenderPipelines.FILLED_BOX_THROUGH_WALLS,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
        //#endif
    )

    @JvmField
    val LINES: RenderLayer = RenderLayer.of(
        "lines",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.LINES)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SboRenderPipelines.LINES,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .lineWidth(RenderPhase.LineWidth(OptionalDouble.empty()))
            .build(false)
        //#endif
    )

    @JvmField
    val LINES_THROUGH_WALLS: RenderLayer = RenderLayer.of(
        "sbo/lines_through_walls",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.LINES_THROUGH_WALLS)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        SboRenderPipelines.LINES_THROUGH_WALLS,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .lineWidth(RenderPhase.LineWidth(OptionalDouble.empty()))
            .build(false)
        //#endif
    )

    val BEACON_BEAM_OPAQUE: RenderLayer = RenderLayer.of(
        "beacon_beam_opaque",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_OPAQUE)
        //$$     .texture("Sampler0", BeaconBlockEntityRenderer.BEAM_TEXTURE)
        //$$     .build()
        //#else
        1536,
        false,
        true,
        SboRenderPipelines.BEACON_BEAM_OPAQUE,
        RenderLayer.MultiPhaseParameters.builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    false
                )
            )
            .build(false)
        //#endif
    )

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderLayer = RenderLayer.of(
        "beacon_beam_opaque_through_walls",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_OPAQUE_THROUGH_WALLS)
        //$$     .texture("Sampler0", BeaconBlockEntityRenderer.BEAM_TEXTURE)
        //$$     .build()
        //#else
        1536,
        false,
        true,
        SboRenderPipelines.BEACON_BEAM_OPAQUE_THROUGH_WALLS,
        RenderLayer.MultiPhaseParameters.builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    false
                )
            )
            .build(false)
        //#endif
    )

    val BEACON_BEAM_TRANSLUCENT: RenderLayer = RenderLayer.of(
        "beacon_beam_translucent",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_TRANSLUCENT)
        //$$     .texture("Sampler0", BeaconBlockEntityRenderer.BEAM_TEXTURE)
        //$$     .translucent()
        //$$     .build()
        //#else
        1536,
        false,
        true,
        SboRenderPipelines.BEACON_BEAM_TRANSLUCENT,
        RenderLayer.MultiPhaseParameters.builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    false
                )
            )
            .build(false)
        //#endif
    )

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderLayer = RenderLayer.of(
        "devonian_beacon_beam_translucent_esp",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS)
        //$$     .texture("Sampler0", BeaconBlockEntityRenderer.BEAM_TEXTURE)
        //$$     .translucent()
        //$$     .build()
        //#else
        1536,
        false,
        true,
        SboRenderPipelines.BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS,
        RenderLayer.MultiPhaseParameters.builder()
            .texture(
                RenderPhase.Texture(
                    BeaconBlockEntityRenderer.BEAM_TEXTURE,
                    false
                )
            )
            .build(false)
        //#endif
    )
}
