package net.sbo.mod.utils.render

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.sbo.mod.utils.waypoint.WaypointManager

object WaypointRenderer : LevelRenderEvents.BeforeTranslucentTerrain {
    override fun beforeTranslucentTerrain(context: LevelRenderContext) {
        WaypointManager.renderAllWaypoints(context)
    }
}
