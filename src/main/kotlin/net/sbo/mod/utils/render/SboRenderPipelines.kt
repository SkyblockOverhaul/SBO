package net.sbo.mod.utils.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
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
            .build()
    )

    val LINES: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(SBOKotlin.id("pipeline/line_strip"))
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, Mode.LINES)
            .withCull(false)
            .build()
    )

    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(SBOKotlin.id("pipeline/line_through_walls"))
            .withShaderDefine("shad")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, Mode.LINES)
            .withCull(false)
            .build()
    )

    val BEACON_BEAM_OPAQUE: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_opaque"))
        .build()

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_opaque_through_walls"))
        .build()

    val BEACON_BEAM_TRANSLUCENT: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_translucent"))
        .build()

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_translucent_through_walls"))
        .build()
}
