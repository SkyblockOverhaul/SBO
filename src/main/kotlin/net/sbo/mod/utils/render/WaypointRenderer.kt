package net.sbo.mod.utils.render

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.sbo.mod.utils.waypoint.WaypointManager

object WaypointRenderer : WorldRenderEvents.BeforeTranslucent {
    override fun beforeTranslucent(context: WorldRenderContext) {
        WaypointManager.renderAllWaypoints(context)
    }
}
