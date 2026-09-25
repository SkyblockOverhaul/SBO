package net.sbo.mod.utils.render

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.sbo.mod.utils.waypoint.WaypointManager

//#if MC < 26.2
object WaypointRenderer : LevelRenderEvents.CollectSubmits, LevelRenderEvents.AfterTranslucentTerrain {
//#else
//$$ object WaypointRenderer : LevelRenderEvents.CollectSubmits {
//#endif
    override fun collectSubmits(context: LevelRenderContext) {
        WaypointManager.renderAllWaypoints(context)
    }

    //#if MC < 26.2
    override fun afterTranslucentTerrain(context: LevelRenderContext) {
        RenderUtils3D.flushLegacyDrawString(context)
    }
    //#endif
}
