package net.sbo.mod.utils.render

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.sbo.mod.utils.waypoint.WaypointManager

//#if MC > 1.21.11
//$$ object WaypointRenderer : WorldRenderEvents.BeforeTranslucent, WorldRenderEvents.CollectSubmits {
//#else
object WaypointRenderer : WorldRenderEvents.BeforeTranslucent {
//#endif
    override fun beforeTranslucent(context: WorldRenderContext) {
        //#if MC < 26.1
        WaypointManager.renderAllWaypoints(context)
        //#endif
    }

    //#if MC > 1.21.11
    //$$ override fun collectSubmits(context: WorldRenderContext) {
    //#else
    fun collectSubmits(context: WorldRenderContext) {
    //#endif
        WaypointManager.renderAllWaypoints(context)
    }
}
