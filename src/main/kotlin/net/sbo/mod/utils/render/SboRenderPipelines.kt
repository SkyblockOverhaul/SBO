package net.sbo.mod.utils.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
//#if MC < 26.1
import com.mojang.blaze3d.platform.DepthTestFunction
//#endif
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat.Mode
import net.minecraft.client.renderer.RenderPipelines
import net.sbo.mod.SBOKotlin

/** Add new pipelines to [net.sbo.mod.compat.IrisCompatibility] */
object SboRenderPipelines {
    val FILLED_BOX_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(SBOKotlin.id("pipeline/debug_filled_box_through_walls"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLE_STRIP)
            //#if MC < 26.1
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            //#endif
            .build()
    )

    val LINES: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(SBOKotlin.id("pipeline/line_strip"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, Mode.LINES)
            .withCull(false)
            //#if MC < 26.1
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(true)
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            //#endif
            .build()
    )

    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(SBOKotlin.id("pipeline/line_through_walls"))
            .withShaderDefine("shad")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, Mode.LINES)
            .withCull(false)
            //#if MC < 26.1
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            //#endif
            .build()
    )

    val BEACON_BEAM_OPAQUE: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_opaque"))
        .build()

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_opaque_through_walls"))
        //#if MC < 26.1
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        //#endif
        .build()

    val BEACON_BEAM_TRANSLUCENT: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_translucent"))
        //#if MC < 26.1
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        //#endif
        .build()

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_translucent_through_walls"))
        //#if MC < 26.1
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        //#endif
        .build()
}
