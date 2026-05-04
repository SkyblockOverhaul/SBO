package net.sbo.mod.utils.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat.Mode
import net.minecraft.client.renderer.RenderPipelines
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.sbo.mod.SBOKotlin

/** Add new pipelines to [net.sbo.mod.compat.IrisCompatibility] */
object SboRenderPipelines {
    val FILLED_BOX_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(SBOKotlin.id("pipeline/debug_filled_box_through_walls"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val LINES: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(*arrayOf<RenderPipeline.Snippet?>(RenderPipelines.LINES_SNIPPET))
            .withLocation(SBOKotlin.id("pipeline/line_strip"))
            //#if MC > 1.21.10
            //$$ .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, DrawMode.LINES)
            //#else
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, Mode.LINES)
            //#endif
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build()
    )

    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(*arrayOf<RenderPipeline.Snippet?>(RenderPipelines.LINES_SNIPPET))
            .withLocation(SBOKotlin.id("pipeline/line_through_walls"))
            .withShaderDefine("shad")
            //#if MC > 1.21.10
            //$$ .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, DrawMode.LINES)
            //#else
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL, Mode.LINES)
            //#endif
            .withCull(false)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val BEACON_BEAM_OPAQUE: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_opaque"))
        .build()

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_opaque_through_walls"))
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()

    val BEACON_BEAM_TRANSLUCENT: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_translucent"))
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .build()

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_translucent_through_walls"))
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()
}