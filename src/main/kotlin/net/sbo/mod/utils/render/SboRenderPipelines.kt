package net.sbo.mod.utils.render

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
//#if MC > 1.21.11
//$$ import com.mojang.blaze3d.pipeline.DepthStencilState
//$$ import com.mojang.blaze3d.platform.CompareOp
//#else
import com.mojang.blaze3d.platform.DepthTestFunction
//#endif
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat.Mode
import net.minecraft.client.renderer.RenderPipelines
import net.sbo.mod.SBOKotlin

/** Add new pipelines to [net.sbo.mod.compat.IrisCompatibility] */
object SboRenderPipelines {
    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(SBOKotlin.id("pipeline/line_through_walls"))
            //#if MC > 1.21.11
            //$$ .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false, 0.0f, 0.0f))
            //#else
            .withCull(false)
            .withShaderDefine("shad")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, Mode.LINES)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withDepthWrite(false)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            //#endif
            .build()
    )

    //#if MC < 26.1
    val BEACON_BEAM_OPAQUE_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_opaque_through_walls"))
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS: RenderPipeline = RenderPipeline.builder(RenderPipelines.BEACON_BEAM_SNIPPET)
        .withLocation(SBOKotlin.id("beacon_beam_translucent_through_walls"))
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()
    //#endif
}
