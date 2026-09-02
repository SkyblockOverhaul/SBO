package net.sbo.mod.utils.render

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.sbo.mod.utils.waypoint.WaypointManager
import net.sbo.mod.utils.render.RenderUtils3D

object WaypointRenderer : LevelRenderEvents.CollectSubmits, LevelRenderEvents.AfterTranslucentTerrain {
    override fun collectSubmits(context: LevelRenderContext) {
        WaypointManager.renderAllWaypoints(context)
    }

    override fun afterTranslucentTerrain(context: LevelRenderContext) {
        RenderUtils3D.flushLegacyDrawString(context)
    }
}
