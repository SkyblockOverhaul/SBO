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
//#if MC > 26.1
//$$ import net.minecraft.util.FormattedCharSequence
//#endif
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Waypoints
import net.sbo.mod.utils.math.SboVec
import java.awt.Color
import kotlin.math.max
import kotlin.math.sqrt
//#if MC < 26.3
import org.joml.Matrix4f
//#endif
//#if MC > 26.1 && MC < 26.3
//$$ import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases
//$$ import net.minecraft.client.renderer.feature.TextFeatureRenderer
//#endif

object RenderUtils3D {
    fun renderWaypoint(
        context: LevelRenderContext,
        renderText: Boolean,
        //#if MC < 26.2
        text: Component,
        //#endif
        textWidth: Int,
        //#if MC > 26.1
        //$$ visualOrderText: FormattedCharSequence,
        //#endif
        //#if MC < 26.2
        secondaryText: Component?,
        //#endif
        secondaryTextWidth: Int,
        //#if MC > 26.1
        //$$ secondaryVisualOrderText: FormattedCharSequence?,
        //#endif
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
                if (Waypoints.lookAlike) 1.0 else 1.5,
                0.0,
                //#if MC < 26.2
                text,
                //#endif
                textWidth,
                //#if MC > 26.1
                //$$ visualOrderText,
                //#endif
                hexColor,
                Waypoints.waypointTextShadow,
                if (Waypoints.lookAlike) 1.5 * Waypoints.waypointTextScale / 12.0 else Waypoints.waypointTextScale / 100.0
            )

            //#if MC > 26.1
            //$$ if (secondaryVisualOrderText != null) {
            //#else
            if (secondaryText != null) {
            //#endif
                drawString(
                    context,
                    pos,
                    if (Waypoints.lookAlike) 1.0 else 11.5,
                    if (Waypoints.lookAlike) 10.0 else 0.0,
                    //#if MC < 26.2
                    secondaryText,
                    //#endif
                    secondaryTextWidth,
                    //#if MC > 26.1
                    //$$ secondaryVisualOrderText,
                    //#endif
                    hexColor,
                    Waypoints.waypointTextShadow,
                    if (Waypoints.lookAlike) 1.7 * Waypoints.waypointTextScale / 12.0 else Waypoints.waypointTextScale / 100.0
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

    //#if MC < 26.2
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
    //#endif

    /**
     * Draws a string in the 3D world that always faces the player.
     * @param context The render context used for transformations.
     * @param pos The position in the world where the text should be drawn.
     * @param worldYOffset The vertical offset of the text in world space.
     * @param screenYOffset The vertical offset of the text in screen space, used when [Waypoints.lookAlike] is enabled.
     * @param text The text [Component] to draw. Only used in Minecraft versions below 26.2, above 26.2 the visualOrderText is used instead.
     * @param textWidth The width of the text in pixels, used to center it horizontally.
     * @param visualOrderText The text in visual order for rendering. Only used in Minecraft versions 26.2 and above, lower versions use the text parameter (a [Component]).
     * @param color The color of the text in ARGB format.
     * @param shadow Whether to draw the text with a shadow.
     * @param scale The scale of the text.
     */
    private fun drawString(
        context: LevelRenderContext,
        pos: SboVec,
        worldYOffset: Double,
        screenYOffset: Double,
        //#if MC < 26.2
        text: Component,
        //#endif
        textWidth: Int,
        //#if MC > 26.1
        //$$ visualOrderText: FormattedCharSequence,
        //#endif
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
            val maxTextDistance = (mc.options.renderDistance().get() * 16.0 - 4.0).coerceAtLeast(5.0)
            val textPos = Vec3(pos.x + 0.5, pos.y + 1.5, pos.z + 0.5)
            val textOffset = textPos.subtract(cameraPos)
            val textDistance = textOffset.length()
            val effectiveDistance = minOf(distance, maxTextDistance)

            val dynamicScale = if (Waypoints.lookAlike) {
                distRender * scale * 0.05
            } else {
                max(effectiveDistance, 2.5) * scale
            }

            val renderPos = if (Waypoints.lookAlike) {
                val compression = distRender / distToPlayer

                Vec3(
                    cameraPos.x + dX * compression,
                    cameraPos.y + eyeHeight + (y + 20.0 * distToPlayer / 300.0 - (cameraPos.y + eyeHeight)) * compression,
                    cameraPos.z + dZ * compression
                )
            } else {
                if (textDistance > maxTextDistance) {
                    cameraPos.add(textOffset.scale(maxTextDistance / textDistance))
                } else {
                    textPos
                }
            }

            translate(renderPos.x - cameraPos.x, renderPos.y - cameraPos.y, renderPos.z - cameraPos.z)

            mulPose(Axis.YP.rotationDegrees(-cameraYaw))
            mulPose(Axis.XP.rotationDegrees(cameraPitch))

            if (Waypoints.lookAlike) translate(0.0, -screenYOffset * dynamicScale, 0.0)

            scale(-dynamicScale.toFloat(), -dynamicScale.toFloat(), dynamicScale.toFloat())

            val xOffset = -textWidth / 2f
            val yOffset = 0f

            //#if MC > 26.1
            //$$ submitText(context, xOffset, yOffset, visualOrderText, color, shadow)
            //#else
            submitText(context, xOffset, yOffset, text, color, shadow)
            //#endif
        }
    }

    /**
     * Draws a camera-facing label like a vanilla name tag, rendering through walls.
     * It keeps the vanilla name tag size up close and grows with distance so it stays readable.
     * @param context The world render context.
     * @param pos The exact world position of the label's bottom edge, usually an interpolated entity position.
     * @param text The text to draw, colors and styles come from the component.
     * @param linesUp How many text lines above [pos] the label is drawn, so it can stack on top of an existing name tag.
     */
    fun drawLabel(context: LevelRenderContext, pos: Vec3, text: Component, linesUp: Int = 0) {
        context.pushPop {
            val camera = context.getCamera()
            val cameraPos = camera.position()
            val growth = (cameraPos.distanceTo(pos) / LABEL_VANILLA_SIZE_DISTANCE).coerceIn(1.0, LABEL_MAX_GROWTH)
            val labelScale = (VANILLA_NAME_TAG_SCALE * growth).toFloat()

            // Variable names must match the 26.3 replacements in build.gradle.kts
            val cameraYaw = camera.yRot()
            val cameraPitch = camera.xRot()

            translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z)
            mulPose(Axis.YP.rotationDegrees(-cameraYaw))
            mulPose(Axis.XP.rotationDegrees(cameraPitch))
            scale(-labelScale, -labelScale, labelScale)

            val visualOrderText = text.visualOrderText
            val yOffset = -(mc.font.lineHeight * (linesUp + 1) + linesUp).toFloat()
            //#if MC > 26.1
            //$$ submitText(context, -mc.font.width(visualOrderText) / 2f, yOffset, visualOrderText, -1, true)
            //#else
            submitText(context, -mc.font.width(visualOrderText) / 2f, yOffset, text, -1, true)
            //#endif
        }
    }

    /** Lines are cut this far in front of the camera, a bit past the near plane (0.05). See [drawLine]. */
    private const val LINE_NEAR_PLANE = 0.1
    /** How far in front of the camera the line from the crosshair starts, see [drawLineFromCursor]. */
    private const val CURSOR_LINE_START_DISTANCE = 32.0
    private const val VANILLA_NAME_TAG_SCALE = 0.025
    private const val LABEL_VANILLA_SIZE_DISTANCE = 16.0
    private const val LABEL_MAX_GROWTH = 5.0

    /**
     * Submits already transformed text with the current pose of [context]'s pose stack, rendering through walls.
     */
    private fun submitText(
        context: LevelRenderContext,
        xOffset: Float,
        yOffset: Float,
        //#if MC < 26.2
        text: Component,
        //#endif
        //#if MC > 26.1
        //$$ visualOrderText: FormattedCharSequence,
        //#endif
        color: Int,
        shadow: Boolean
    ) {
        val layerType = Font.DisplayMode.SEE_THROUGH
        //#if MC > 26.1
        //$$ val outlineColor = 0 // NOTE: has to be zero or otherwise SEE_THROUGH won't work since buildGroup overrides it to NORMAL and then POLYGLON_OFFSET if outlineColor is not zero. So zero is required to render the text through walls.
        //#endif

        val backgroundColor = 0

        val packedLightCoords = mc.entityRenderDispatcher.getPackedLightCoords(mc.player!!, mc.deltaTracker.getGameTimeDeltaPartialTick(true))

        // Workaround for https://mojira.dev/MC-298659
        // Adapted from https://github.com/hannibal002/SkyHanni/pull/6458

        // 26.3 and above: Use submitText triggered from COLLECT_SUBMITS and submitted at COLLECT_SUBMITS since bug is fixed.
        // 26.2: Use submitCustom triggered from COLLECT_SUBMITS and submitted at SubmitRenderPhases.AFTER_TERRAIN with TextFeatureRenderer.Submit.
        // 26.1.2: Add to a list triggered from COLLECT_SUBMITS which submits at AFTER_TRANSLUCENT_TERRAIN later with the legacy Font#drawInBatch method.

        //#if MC > 26.2
        //$$ context.submitNodeCollector().submitText(context.poseStack(), xOffset, yOffset, visualOrderText, shadow, layerType, packedLightCoords, color, backgroundColor, outlineColor)
        //#elseif MC > 26.1
        //$$ context.submitNodeCollector().submitCustom(
        //$$    SubmitRenderPhases.AFTER_TERRAIN,
        //$$    TextFeatureRenderer.Submit(
        //$$        Matrix4f(context.poseStack().last().pose()),
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
                Matrix4f(context.poseStack().last().pose())
            )
        )
        //#endif
    }

    //#if MC < 26.2
    fun flushLegacyDrawString(context: LevelRenderContext) {
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
    }
    //#endif

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
        alpha: Float
    ) {
        val camera = context.getCamera()

        // Any point straight ahead lands on the crosshair and gives the same line on screen. A far one makes a line to
        // a target behind the player reach the screen edge: from 1 block ahead it passes so close to the camera that
        // [drawLine] cuts it off in the middle of the screen.
        val startPos = camera.position()
            .add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()).scale(CURSOR_LINE_START_DISTANCE))

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
     * Draws a line from a start point to a target point in the world. The part behind the camera is cut off.
     * @param context The world render context.
     * @param startPos The start position in the world.
     * @param endPos The end position in the world.
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
        alpha: Float
    ) {
        val camera = context.getCamera()

        // Made camera relative in doubles: world coordinates as floats lose precision far from 0 0.
        var start = startPos.subtract(camera.position())
        var end = endPos.subtract(camera.position())

        // The lines shader widens a line on screen from its projected end points. An end point behind the camera
        // projects to a flipped, meaningless spot, which made the line wobble whenever its target was behind the
        // player. Cut the line at a plane just in front of the camera, the part behind it is not visible anyway.
        val forward = Vec3.directionFromRotation(camera.xRot(), camera.yRot())
        val startDepth = start.dot(forward)
        val endDepth = end.dot(forward)
        if (startDepth < LINE_NEAR_PLANE && endDepth < LINE_NEAR_PLANE) return
        if (startDepth < LINE_NEAR_PLANE) {
            start = start.lerp(end, (LINE_NEAR_PLANE - startDepth) / (endDepth - startDepth))
        } else if (endDepth < LINE_NEAR_PLANE) {
            end = end.lerp(start, (LINE_NEAR_PLANE - endDepth) / (startDepth - endDepth))
        }

        // The lines shader uses the normal as the line direction: it projects position + normal to get the direction
        // on screen and widens the line sideways from it. Any other normal makes the line wobble with the view angle.
        // Only its axis matters, so it points away from the camera: position + normal then never lands behind it.
        var dir = end.subtract(start).normalize()
        if (dir.dot(forward) < 0) dir = dir.reverse()
        val nx = dir.x.toFloat()
        val ny = dir.y.toFloat()
        val nz = dir.z.toFloat()

        context.submitNodeCollector().submitCustomGeometry(
            context.poseStack(),
            SboRenderLayers.LINES_THROUGH_WALLS
        ) { pose, consumer ->
            consumer
                .addVertex(pose, start.x.toFloat(), start.y.toFloat(), start.z.toFloat())
                .setNormal(pose, nx, ny, nz)
                .setColor(color[0], color[1], color[2], alpha)
                .setLineWidth(lineWidth)

            consumer
                .addVertex(pose, end.x.toFloat(), end.y.toFloat(), end.z.toFloat())
                .setNormal(pose, nx, ny, nz)
                .setColor(color[0], color[1], color[2], alpha)
                .setLineWidth(lineWidth)
        }
    }

    /**
     * Renders a beacon beam at the given location.
     * @param ctx The world render context.
     * @param vec The position in the world where the beacon beam should be rendered.
     * @param colorComponents The RGB color components as a FloatArray (0.0 to
     */
    private fun renderBeaconBeam(
        ctx: LevelRenderContext,
        vec: SboVec,
        colorComponents: FloatArray
    ) {
        val player = mc.player ?: return
        if (vec.center().distanceTo(player.x, player.y, player.z) < Waypoints.beamDistance) return

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

    private inline fun LevelRenderContext.pushPop(function: PoseStack.() -> Unit) {
        val matrix = poseStack()
        matrix.pushPop(function)
    }

    private inline fun PoseStack.pushPop(function: PoseStack.() -> Unit) {
        this.pushPose()
        function()
        this.popPose()
    }
}
