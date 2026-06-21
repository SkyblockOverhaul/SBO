package net.sbo.mod.utils.render

import net.sbo.mod.utils.chat.ChatUtils
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Camera
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.utils.math.SboVec
import java.awt.Color
import kotlin.math.max
import kotlin.math.sqrt

object RenderUtils3D {
    fun renderWaypoint(
        context: LevelRenderContext,
        text: String,
        pos: SboVec,
        colorComponents: FloatArray,
        hexColor: Int,
        alpha: Float,
        drawLine: Boolean,
        lineWidth: Float,
        renderBeam: Boolean
    ) {
        drawFilledBox(
            pos,
            colorComponents,
            alpha
        )

        if (drawLine) {
            drawLineFromCursor(
                context,
                pos,
                colorComponents,
                lineWidth,
                alpha
            )
        }

        if (renderBeam) {
            renderBeaconBeam(
                context,
                pos,
                colorComponents
            )
        }

        if (text.isNotEmpty() && text != "§7") {
            drawString(
                context,
                pos,
                1.5,
                text,
                hexColor,
                Customization.waypointTextShadow,
                Customization.waypointTextScale/100.0
            )
        }
    }

    /**
     * Draws a filled box at the specified world coordinates.
     * @param pos The position in the world where the box should be drawn.
     * @param colorComponents The RGB color components as a FloatArray (0.0 to 1.0).
     * @param alpha The alpha value for transparency (0.0 to 1.0).
     */
    private fun drawFilledBox(
        pos: SboVec,
        colorComponents: FloatArray,
        alpha: Float,
    ) {
        val r = (colorComponents[0].coerceIn(0f, 1f) * 255).toInt()
        val g = (colorComponents[1].coerceIn(0f, 1f) * 255).toInt()
        val b = (colorComponents[2].coerceIn(0f, 1f) * 255).toInt()
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        val argbColor = (a shl 24) or (r shl 16) or (g shl 8) or b
        val bPos = pos.toBlockPos().immutable()
        Gizmos.cuboid(AABB.encapsulatingFullBlocks(bPos, bPos), GizmoStyle.fill(argbColor)).setAlwaysOnTop()
    }

    /**
     * Draws a string in the 3D world that always faces the player.
     * @param context The matrix stack for transformations.
     * @param pos The position in the world where the text should be drawn.
     * @param text The text to draw.
     * @param color The color of the text in ARGB format.
     * @param shadow Whether to draw the text with a shadow.
     * @param scale The scale of the text.
     */
    private fun drawString(
        context: LevelRenderContext,
        pos: SboVec,
        yOffset: Double,
        text: String,
        color: Int,
        shadow: Boolean,
        scale: Double
    ) {
        context.pushPop {
            val camera = context.getCamera()
            val cameraPos = camera.position()
            val cameraYaw = camera.yRot()
            val cameraPitch = camera.xRot()
            val textRenderer = mc.font

            val textWorldPos = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            val distance = cameraPos.distanceTo(textWorldPos)
            val dynamicScale = max(distance, 2.5) * scale

            translate(pos.x + 0.5 - cameraPos.x, pos.y + yOffset + - cameraPos.y, pos.z + 0.5 - cameraPos.z)

            mulPose(Axis.YP.rotationDegrees(-cameraYaw))
            mulPose(Axis.XP.rotationDegrees(cameraPitch))

            scale(-dynamicScale.toFloat(), -dynamicScale.toFloat(), dynamicScale.toFloat())

            val textWidth = textRenderer.width(text)
            val xOffset = -textWidth / 2f

            val layerType = Font.DisplayMode.SEE_THROUGH

            val deltaPartialTick = mc.deltaTracker.getGameTimeDeltaPartialTick(true)
            val visualOrderText = ChatUtils.fromLegacy(text).visualOrderText

            context.submitNodeCollector().submitText(context.poseStack(), xOffset, 0f, visualOrderText, shadow, layerType, mc.entityRenderDispatcher.getPackedLightCoords(mc.player!!, deltaPartialTick), color, 0, 0xF000F0)
        }
    }

    /**
     * Draws a line from the player's eyes to a target point in the world.
     * @param context The world render context.
     * @param target The target position in the world.
     * @param color The RGB color of the line as a FloatArray (0.0 to 1.0).
     * @param lineWidth The width of the line.
     * @param alpha The alpha value for transparency (0.0 to 1.0).
     */
    fun drawLineFromCursor(
        context: LevelRenderContext,
        target: SboVec,
        color: FloatArray,
        lineWidth: Float,
        alpha: Float = 0.5f
    ) {
        context.pushPop {
            val camera = context.getCamera()
            val cameraPos = camera.position()

            translate(cameraPos.reverse())

            val startPos =
                cameraPos.add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()))

            val endPos =
                target.center().toVec3d().add(0.0, 0.5, 0.0)

            val lineDir = endPos.subtract(startPos)
            val viewDir = startPos.subtract(cameraPos)

            val sideVec = lineDir.cross(viewDir).normalize()
            val upVec = sideVec.cross(lineDir).normalize()

            val nx = upVec.x.toFloat()
            val ny = upVec.y.toFloat()
            val nz = upVec.z.toFloat()

            val renderLayer = RenderTypes.LINES

            context.submitNodeCollector().submitCustomGeometry(
                context.poseStack(),
                renderLayer
            ) { pose, consumer ->
                consumer
                    .addVertex(
                        pose,
                        startPos.x.toFloat(),
                        startPos.y.toFloat(),
                        startPos.z.toFloat()
                    )
                    .setNormal(pose, nx, ny, nz)
                    .setColor(color[0], color[1], color[2], alpha)
                    .setLineWidth(lineWidth)

                consumer
                    .addVertex(
                        pose,
                        endPos.x.toFloat(),
                        endPos.y.toFloat(),
                        endPos.z.toFloat()
                    )
                    .setNormal(pose, nx, ny, nz)
                    .setColor(color[0], color[1], color[2], alpha)
                    .setLineWidth(lineWidth)
            }
        }
    }

    /**
     * Renders a beacon beam at the given location.
     * @param ctx The world render context.
     * @param vec The position in the world where the beacon beam should be rendered.
     * @param colorComponents The RGB color components as a FloatArray (0.0 to
     */
    fun renderBeaconBeam(
        ctx: LevelRenderContext,
        vec: SboVec,
        colorComponents: FloatArray
    ) {
        val player = mc.player ?: return
        if (vec.center().distanceTo(player.x, player.y, player.z) < 8) return

        val world = mc.level ?: return

        val beamColor = floatArrayOf(colorComponents[0], colorComponents[1], colorComponents[2], 1.0f)
        val cameraPos = ctx.getCamera().position()

        ctx.pushPop {
            translate(
                vec.x - cameraPos.x,
                (vec.y + 1.0) - cameraPos.y,
                vec.z - cameraPos.z
            )

            BeaconRenderer.submitBeaconBeam(
                ctx.poseStack(),
                ctx.submitNodeCollector(),
                BeaconRenderer.BEAM_LOCATION,
                1.0f,
                Math.floorMod(world.gameTime, 40)
                    + mc.deltaTracker.getGameTimeDeltaPartialTick(false),
                0,
                320,
                Color(
                    beamColor[0],
                    beamColor[1],
                    beamColor[2]
                ).rgb,
                0.2f,
                0.25f
            )
        }
    }

    private fun LevelRenderContext.getCamera(): Camera {
        return gameRenderer().mainCamera
    }

    private fun LevelRenderContext.pushPop(function: PoseStack.() -> Unit) {
        val matrix = poseStack()
        matrix.pushPop(function)
    }

    private fun PoseStack.pushPop(function: PoseStack.() -> Unit) {
        this.pushPose()
        function()
        this.popPose()
    }
}
