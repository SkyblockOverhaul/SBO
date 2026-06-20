package net.sbo.mod.compat

import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.api.v0.IrisProgram
import net.sbo.mod.utils.render.SboRenderPipelines

object IrisCompatibility {
    fun init() {
        IrisApi.getInstance().apply {
            assignPipeline(SboRenderPipelines.LINES_THROUGH_WALLS, IrisProgram.LINES)
            assignPipeline(SboRenderPipelines.BEACON_BEAM_OPAQUE_THROUGH_WALLS, IrisProgram.BEACON_BEAM)
            assignPipeline(SboRenderPipelines.BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS, IrisProgram.BEACON_BEAM)
        }
    }
}
