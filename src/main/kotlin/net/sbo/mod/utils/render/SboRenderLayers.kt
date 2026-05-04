package net.sbo.mod.utils.render

import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.util.TriState
import java.util.OptionalDouble
//#if MC > 1.21.10
//$$ import net.minecraft.client.render.RenderSetup
//$$ import net.minecraft.client.render.LayeringTransform
//#else
import net.minecraft.client.renderer.RenderStateShard
//#endif

object SboRenderLayers {
    @JvmField
    val FILLED_BOX: RenderType = RenderType.create(
        "sbo/filled_box",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(RenderPipelines.DEBUG_FILLED_BOX)
        //$$     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        RenderPipelines.DEBUG_FILLED_BOX,
        RenderType.CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
        //#endif
    )

    @JvmField
    val FILLED_BOX_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/filled_box_through_walls",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.FILLED_BOX_THROUGH_WALLS)
        //$$     .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SboRenderPipelines.FILLED_BOX_THROUGH_WALLS,
        RenderType.CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
        //#endif
    )

    @JvmField
    val LINES: RenderType = RenderType.create(
        "lines",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.LINES)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SboRenderPipelines.LINES,
        RenderType.CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .createCompositeState(false)
        //#endif
    )

    @JvmField
    val LINES_THROUGH_WALLS: RenderType = RenderType.create(
        "sbo/lines_through_walls",
        //#if MC > 1.21.10
        //$$ RenderSetup.builder(SboRenderPipelines.LINES_THROUGH_WALLS)
        //$$     .translucent()
        //$$     .build()
        //#else
        RenderType.TRANSIENT_BUFFER_SIZE,
        false,
        true,
        SboRenderPipelines.LINES_THROUGH_WALLS,
        RenderType.CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .setLineState(RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .createCompositeState(false)
        //#endif
    )

    val BEACON_BEAM_OPAQUE: RenderType = RenderType.create(
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
        RenderType.CompositeState.builder()
            .setTextureState(
                RenderStateShard.TextureStateShard(
                    BeaconRenderer.BEAM_LOCATION,
                    false
                )
            )
            .createCompositeState(false)
        //#endif
    )

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderType = RenderType.create(
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
        RenderType.CompositeState.builder()
            .setTextureState(
                RenderStateShard.TextureStateShard(
                    BeaconRenderer.BEAM_LOCATION,
                    false
                )
            )
            .createCompositeState(false)
        //#endif
    )

    val BEACON_BEAM_TRANSLUCENT: RenderType = RenderType.create(
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
        RenderType.CompositeState.builder()
            .setTextureState(
                RenderStateShard.TextureStateShard(
                    BeaconRenderer.BEAM_LOCATION,
                    false
                )
            )
            .createCompositeState(false)
        //#endif
    )

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderType = RenderType.create(
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
        RenderType.CompositeState.builder()
            .setTextureState(
                RenderStateShard.TextureStateShard(
                    BeaconRenderer.BEAM_LOCATION,
                    false
                )
            )
            .createCompositeState(false)
        //#endif
    )
}
