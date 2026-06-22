package net.sbo.mod.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.Camera
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.texture.OverlayTexture
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

object RenderUtils3D {
    fun renderWaypoint(
        context: WorldRenderContext,
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
        context: WorldRenderContext,
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

            val consumers = context.consumers()

            val layerType = Font.DisplayMode.SEE_THROUGH

            textRenderer.drawInBatch(
                text,
                xOffset,
                0f,
                color,
                shadow,
                last().pose(),
                consumers,
                layerType,
                0,
                0xF000F0
            )
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
    private fun drawLineFromCursor(
        context: WorldRenderContext,
        target: SboVec,
        color: FloatArray,
        lineWidth: Float,
        alpha: Float = 0.5f
    ) {
        context.pushPop {
            val camera = context.getCamera()
            val cameraPos = camera.position()

            translate(cameraPos.reverse())

            val consumers = context.consumers()
            val startPos = cameraPos.add(Vec3.directionFromRotation(camera.xRot(), camera.yRot()))
            val endPos = target.center().toVec3d().add(0.0, 0.5, 0.0)

            val lineDir = endPos.subtract(startPos)
            val viewDir = startPos.subtract(cameraPos)

            val sideVec = lineDir.cross(viewDir).normalize()

            val upVec = sideVec.cross(lineDir).normalize()

            val nx = upVec.x.toFloat()
            val ny = upVec.y.toFloat()
            val nz = upVec.z.toFloat()

            val renderLayer = SboRenderLayers.LINES_THROUGH_WALLS
            val buffer = consumers.getBuffer(renderLayer)
            val matrixEntry = last()

            buffer.addVertex(matrixEntry, startPos.x.toFloat(), startPos.y.toFloat(), startPos.z.toFloat())
                .setNormal(matrixEntry, nx, ny, nz)
                .setColor(color[0], color[1], color[2], alpha)
                .setLineWidth(lineWidth)

            buffer.addVertex(matrixEntry, endPos.x.toFloat(), endPos.y.toFloat(), endPos.z.toFloat())
                .setNormal(matrixEntry, nx, ny, nz)
                .setColor(color[0], color[1], color[2], alpha)
                .setLineWidth(lineWidth)
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
        ctx: WorldRenderContext,
        vec: SboVec,
        colorComponents: FloatArray
    ) {
        val player = mc.player ?: return
        if (vec.center().distanceTo(player.x, player.y, player.z) < 8) return

        val consumers = ctx.consumers()
        val world = mc.level ?: return
        val partialTicks = mc.deltaTracker.getGameTimeDeltaPartialTick(true)
        val cam = ctx.getCamera().position()
        val beamColor = floatArrayOf(colorComponents[0], colorComponents[1], colorComponents[2], 1.0f)

        ctx.pushPop {
            translate(vec.x - cam.x, (vec.y + 1.0) - cam.y, vec.z - cam.z)

            renderBeam(
                consumers,
                partialTicks,
                world.gameTime,
                Color(beamColor[0], beamColor[1], beamColor[2]).rgb
            )
        }
    }

    private fun PoseStack.renderBeam(
        vertices: MultiBufferSource,
        partialTicks: Float,
        worldTime: Long,
        color: Int
    ) {
        val opaqueLayer = SboRenderLayers.BEACON_BEAM_OPAQUE_THROUGH_WALLS
        val translucentLayer = SboRenderLayers.BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS
        val heightScale = 1f
        val height = 320
        val innerRadius = 0.2f
        val outerRadius = 0.25f
        val time = Math.floorMod(worldTime, 40) + partialTicks
        val fixedTime = -time
        val wavePhase = Mth.frac(fixedTime * 0.2f - Mth.floor(fixedTime * 0.1f).toFloat())
        val animationStep = -1f + wavePhase
        var renderYOffest = height.toFloat() * heightScale * (0.5f / innerRadius) + animationStep

        pushPop {
            translate(0.5, 0.0, 0.5)

            pushPop {
                mulPose(Axis.YP.rotationDegrees(time * 2.25f - 45f))

                renderBeamLayer(
                    vertices.getBuffer(opaqueLayer),
                    color,
                    0f,
                    innerRadius,
                    innerRadius,
                    0f,
                    -innerRadius,
                    0f,
                    0f,
                    -innerRadius,
                    renderYOffest,
                    animationStep
                )
            }

            renderYOffest = height.toFloat() * heightScale + animationStep

            renderBeamLayer(
                vertices.getBuffer(translucentLayer),
                ARGB.color(32, color),
                -outerRadius,
                -outerRadius,
                outerRadius,
                -outerRadius,
                -outerRadius,
                outerRadius,
                outerRadius,
                outerRadius,
                renderYOffest,
                animationStep
            )
        }
    }

    private fun PoseStack.renderBeamLayer(
        vertices: VertexConsumer,
        color: Int,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        x3: Float,
        z3: Float,
        x4: Float,
        z4: Float,
        v1: Float,
        v2: Float
    ) {
        val entry = last()
        renderBeamFace(entry, vertices, color, x1, z1, x2, z2, v1, v2)
        renderBeamFace(entry, vertices, color, x4, z4, x3, z3, v1, v2)
        renderBeamFace(entry, vertices, color, x2, z2, x4, z4, v1, v2)
        renderBeamFace(entry, vertices, color, x3, z3, x1, z1, v1, v2)
    }

    private fun renderBeamFace(
        matrix: PoseStack.Pose,
        vertices: VertexConsumer,
        color: Int,
        x1: Float,
        z1: Float,
        x2: Float,
        z2: Float,
        v1: Float,
        v2: Float
    ) {
        renderBeamVertex(matrix, vertices, color, 320, x1, z1, 1f, v1)
        renderBeamVertex(matrix, vertices, color, 0, x1, z1, 1f, v2)
        renderBeamVertex(matrix, vertices, color, 0, x2, z2, 0f, v2)
        renderBeamVertex(matrix, vertices, color, 320, x2, z2, 0f, v1)
    }

    private fun renderBeamVertex(
        matrix: PoseStack.Pose,
        vertices: VertexConsumer,
        color: Int,
        y: Int,
        x: Float,
        z: Float,
        u: Float,
        v: Float
    ) {
        vertices
            .addVertex(matrix, x, y.toFloat(), z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880).setNormal(matrix, 0f, 1f, 0f)
    }

    private fun WorldRenderContext.getCamera(): Camera {
        return gameRenderer().mainCamera
    }

    private fun WorldRenderContext.pushPop(function: PoseStack.() -> Unit) {
        val matrix = matrices()
        matrix.pushPop(function)
    }

    private fun PoseStack.pushPop(function: PoseStack.() -> Unit) {
        this.pushPose()
        function()
        this.popPose()
    }
}
