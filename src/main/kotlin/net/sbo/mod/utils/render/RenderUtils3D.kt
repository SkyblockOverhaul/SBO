package net.sbo.mod.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Camera
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.blockentity.BeaconRenderer
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.settings.categories.Diana
import net.sbo.mod.settings.categories.Debug
import net.sbo.mod.utils.math.SboVec
import java.awt.Color
import kotlin.math.max
import kotlin.math.sqrt
import org.joml.Matrix4f
//#if MC > 26.1
//$$ import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases
//$$ import net.minecraft.client.renderer.feature.TextFeatureRenderer
//#endif

object RenderUtils3D {
    fun renderWaypoint(
        context: LevelRenderContext,
        renderText: Boolean,
        text: Component,
        textWidth: Int,
        visualOrderText: FormattedCharSequence,
        secondaryText: Component?,
        secondaryTextWidth: Int,
        secondaryVisualOrderText: FormattedCharSequence?,
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

        if (renderText) {
            drawString(
                context,
                pos,
                if (Customization.lookAlike) 1.0 else 1.5,
                0.0,
                text,
                textWidth,
                visualOrderText,
                hexColor,
                Customization.waypointTextShadow,
                if (Customization.lookAlike) 1.5 * Customization.waypointTextScale / 12.0 else Customization.waypointTextScale / 100.0
            )

            if (secondaryText != null && secondaryVisualOrderText != null) {
                drawString(
                    context,
                    pos,
                    if (Customization.lookAlike) 1.0 else 11.5,
                    if (Customization.lookAlike) 10.0 else 0.0,
                    secondaryText,
                    secondaryTextWidth,
                    secondaryVisualOrderText,
                    hexColor,
                    Customization.waypointTextShadow,
                    if (Customization.lookAlike) 1.7 * Customization.waypointTextScale / 12.0 else Customization.waypointTextScale / 100.0
                )
            }
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
        val argbColor = a shl 24 or (r shl 16) or (g shl 8) or b
        val bPos = pos.toBlockPos().immutable()
        Gizmos.cuboid(AABB.encapsulatingFullBlocks(bPos, bPos), GizmoStyle.fill(argbColor)).setAlwaysOnTop()
    }

    private val legacyDrawString = mutableListOf<DrawInBatchParameters>()

    class DrawInBatchParameters(
        val text: Component,
        val xOffset: Float,
        val yOffset: Float,
        val color: Int,
        val shadow: Boolean,
        val layerType: Font.DisplayMode,
        val backgroundColor: Int,
        val packedLightCoords: Int,
        val pose: Matrix4f
    )

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
        worldYOffset: Double,
        screenYOffset: Double,
        text: Component,
        textWidth: Int,
        visualOrderText: FormattedCharSequence,
        color: Int,
        shadow: Boolean,
        scale: Double
    ) {
        context.pushPop {
            val camera = context.getCamera()
            val cameraPos = camera.position()
            val cameraYaw = camera.yRot()
            val cameraPitch = camera.xRot()

            val textWorldPos = Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            val distance = cameraPos.distanceTo(textWorldPos)

            val player = mc.player!!
            val eyeHeight = player.getEyeHeight(player.pose)

            val x = pos.x + 0.5
            val y = pos.y + worldYOffset
            val z = pos.z + 0.5

            val dX = x - cameraPos.x
            val dY = y - (cameraPos.y + eyeHeight)
            val dZ = z - cameraPos.z

            val distToPlayer = sqrt(dX * dX + dY * dY + dZ * dZ).coerceAtLeast(5.0)
            val distRender = distToPlayer.coerceAtMost(50.0)

            val dynamicScale = if (Customization.lookAlike) {
                distRender * scale * 0.05
            } else {
                max(distance, 2.5) * scale
            }

            val renderPos = if (Customization.lookAlike) {
                val compression = distRender / distToPlayer

                Vec3(
                    cameraPos.x + dX * compression,
                    cameraPos.y + eyeHeight + (y + 20.0 * distToPlayer / 300.0 - (cameraPos.y + eyeHeight)) * compression,
                    cameraPos.z + dZ * compression
                )
            } else {
                Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            }

            translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z)

            mulPose(Axis.YP.rotationDegrees(-cameraYaw))
            mulPose(Axis.XP.rotationDegrees(cameraPitch))

            if (Customization.lookAlike) translate(0.0, -screenYOffset * dynamicScale, 0.0)

            scale(-dynamicScale.toFloat(), -dynamicScale.toFloat(), dynamicScale.toFloat())

            val xOffset = -textWidth / 2f
            val yOffset = 0f

            val layerType = Font.DisplayMode.SEE_THROUGH
            val outlineColor = 0 // NOTE: has to be zero or otherwise SEE_THROUGH won't work since buildGroup overrides it to NORMAL and then POLYGLON_OFFSET if outlineColor is not zero. So zero is required to render the text through walls.

            val backgroundColor = 0

            val packedLightCoords = mc.entityRenderDispatcher.getPackedLightCoords(mc.player!!, mc.deltaTracker.getGameTimeDeltaPartialTick(true))

            // Workaround for https://mojira.dev/MC-298659
            // Adapted from https://github.com/hannibal002/SkyHanni/pull/6458

            // 26.3 and above: Use submitText triggered from COLLECT_SUBMITS and submitted at COLLECT_SUBMITS since bug is fixed.
            // 26.2: Use submitCustom triggered from COLLECT_SUBMITS and submitted at SubmitRenderPhases.AFTER_TERRAIN with TextFeatureRenderer.Submit.
            // 26.1.2: Add to a list triggered from COLLECT_SUBMITS which submits at AFTER_TRANSLUCENT_TERRAIN later with the legacy Font#drawInBatch method, unless Debug.useNodeCollector is true.

            //#if MC > 26.2
            //$$ val useNodeCollector = true
            //#else
            val useNodeCollector = Debug.forceNodeCollector
            //#endif

            if (useNodeCollector) {
                context.submitNodeCollector().submitText(context.poseStack(), xOffset, yOffset, visualOrderText, shadow, layerType, packedLightCoords, color, backgroundColor, outlineColor)
            } else {
                //#if MC > 26.1
                //$$ context.submitNodeCollector().submitCustom(
                //$$    SubmitRenderPhases.AFTER_TERRAIN,
                //$$    TextFeatureRenderer.Submit(
                //$$        Matrix4f(last().pose()),
                //$$        xOffset,
                //$$        yOffset,
                //$$        visualOrderText,
                //$$        shadow,
                //$$        layerType,
                //$$        packedLightCoords,
                //$$        color,
                //$$        backgroundColor,
                //$$        outlineColor,
                //$$    ),
                //$$ )
                //#else
                legacyDrawString.add(
                    DrawInBatchParameters(
                        text,
                        xOffset,
                        yOffset,
                        color,
                        shadow,
                        layerType,
                        backgroundColor,
                        packedLightCoords,
                        Matrix4f(last().pose())
                    )
                )
                //#endif    
            }
        }
    }

    fun flushLegacyDrawString(context: LevelRenderContext) {
        //#if MC > 26.1
        //#else
        val iterator = legacyDrawString.iterator()

        while (iterator.hasNext()) {
            val params = iterator.next()

            mc.font.drawInBatch(
                params.text,
                params.xOffset,
                params.yOffset,
                params.color,
                params.shadow,
                params.pose,
                context.bufferSource(),
                params.layerType,
                params.backgroundColor,
                params.packedLightCoords
            )

            iterator.remove()
        }
        //#endif
    }

    /**
     * Draws a line from the player's eyes to a target point in the world.
     * @param context The world render context.
     * @param target The target position in the world.
     * @param color The RGB color of the line as a FloatArray (0.0 to 1.0).
     * @param lineWidth The width of the line.
     * @param alpha The alpha value for transparency (0.0 to 1.0).
     */
    private fun drawLineFromCursor(
        context: LevelRenderContext,
        target: SboVec,
        color: FloatArray,
        lineWidth: Float,
        alpha: Float = 0.5f
    ) {
        val camera = context.getCamera()

        val startPos = camera.position()
            .add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()))

        val endPos = target.center()
            .toVec3d()
            .add(0.0, 0.5, 0.0)

        drawLine(
            context,
            startPos,
            endPos,
            color,
            lineWidth,
            alpha
        )
    }

    /**
     * Draws a line from a start point to a target point in the world.
     * @param context The world render context.
     * @param startPos The start position in the world.
     * @param target The target position in the world.
     * @param color The RGB color of the line as a FloatArray (0.0 to 1.0).
     * @param lineWidth The width of the line.
     * @param alpha The alpha value for transparency (0.0 to 1.0).
     */
    fun drawLine(
        context: LevelRenderContext,
        startPos: Vec3,
        endPos: Vec3,
        color: FloatArray,
        lineWidth: Float,
        alpha: Float = 0.5f
    ) {
        context.pushPop {
            val cameraPos = context.getCamera().position()

            translate(cameraPos.reverse())

            val lineDir = endPos.subtract(startPos)
            val viewDir = startPos.subtract(cameraPos)

            val sideVec = lineDir.cross(viewDir).normalize()
            val upVec = sideVec.cross(lineDir).normalize()

            val nx = upVec.x.toFloat()
            val ny = upVec.y.toFloat()
            val nz = upVec.z.toFloat()

            val renderLayer = SboRenderLayers.LINES_THROUGH_WALLS

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
     * @param phase Whether the beam should render through walls.
     */
    private fun renderBeaconBeam(
        ctx: LevelRenderContext,
        vec: SboVec,
        colorComponents: FloatArray
    ) {
        val player = mc.player ?: return
        if (vec.center().distanceTo(player.x, player.y, player.z) < Diana.beamDistance) return

        val world = mc.level ?: return
        val partialTicks = mc.deltaTracker.getGameTimeDeltaPartialTick(true)
        val cam = ctx.getCamera().position()
        val beamColor = floatArrayOf(colorComponents[0], colorComponents[1], colorComponents[2], 1.0f)

        ctx.pushPop {
            translate(vec.x - cam.x, vec.y + 1.0 - cam.y, vec.z - cam.z)

            BeaconRenderer.submitBeaconBeam(
                ctx.poseStack(),
                ctx.submitNodeCollector(),
                BeaconRenderer.BEAM_LOCATION,
                1.0f,
                Math.floorMod(world.gameTime, 40)
                    + partialTicks,
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

    private fun LevelRenderContext.getCamera(): Camera = gameRenderer().mainCamera

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
