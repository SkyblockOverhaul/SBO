package net.sbo.mod.utils.render

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.fabric.api.client.rendering.v1.*
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.*
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import net.sbo.mod.SBOKotlin.mc
import net.sbo.mod.settings.categories.Customization
import net.sbo.mod.utils.math.SboVec
import java.awt.Color
import kotlin.math.max
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.ColorHelper
import net.sbo.mod.settings.categories.Diana

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext

//#if MC > 1.21.10
//$$ import net.minecraft.world.debug.gizmo.GizmoDrawing
//$$ import net.minecraft.util.math.Box
//#endif

object RenderUtils3D {
    fun renderWaypoint(
        context: WorldRenderContext,
        text: String,
        pos: SboVec,
        colorComponents: FloatArray,
        hexColor: Int,
        alpha: Float,
        throughWalls: Boolean,
        drawLine: Boolean,
        lineWidth: Float,
        renderBeam: Boolean
    ) {
        drawFilledBox(
            context,
            pos,
            colorComponents,
            alpha,
            throughWalls
        )

        if (drawLine) {
            drawLineFromCursor(
                context,
                pos,
                colorComponents,
                lineWidth,
                throughWalls,
                alpha
            )
        }

        if (renderBeam) {
            renderBeaconBeam(
                context,
                pos,
                colorComponents,
                true
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
                Customization.waypointTextScale/100.0,
                throughWalls
            )
        }
    }

    /**
     * Draws a filled box at the specified world coordinates.
     * @param context The world render context.
     * @param pos The position in the world where the box should be drawn.
     * @param width The width of the box.
     * @param height The height of the box.
     * @param depth The depth of the box.
     * @param colorComponents The RGB color components as a FloatArray (0.0 to 1.0).
     * @param alpha The alpha value for transparency (0.0 to 1.0).
     * @param throughWalls Whether the box should be drawn through walls.
     */
    fun drawFilledBox(
        context: WorldRenderContext,
        pos: SboVec,
        colorComponents: FloatArray,
        alpha: Float,
        throughWalls: Boolean
    ) {
        //#if MC > 1.21.10
        //$$ val r = (colorComponents[0].coerceIn(0f, 1f) * 255).toInt()
        //$$ val g = (colorComponents[1].coerceIn(0f, 1f) * 255).toInt()
        //$$ val b = (colorComponents[2].coerceIn(0f, 1f) * 255).toInt()
        //$$ val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        //$$ val argbColor = (a shl 24) or (r shl 16) or (g shl 8) or b
        //$$ val bPos = pos.toBlockPos().toImmutable()
        //$$ if (throughWalls) {
        //$$     GizmoDrawing.box(Box.enclosing(bPos, bPos), DrawStyle.filled(argbColor)).ignoreOcclusion()
        //$$ } else {
        //$$     GizmoDrawing.box(Box.enclosing(bPos, bPos), DrawStyle.filled(argbColor))
        //$$ }
        //#else
        val width = 1.0
        val height = 1.0
        val depth = 1.0
        context.pushPop {
            val cameraPos = context.getCamera().pos
            translate(pos.x + 0.5 - cameraPos.x, pos.y - cameraPos.y, pos.z + 0.5 - cameraPos.z)

            val consumers = context.consumers()!!

            val renderLayer = if (throughWalls) SboRenderLayers.FILLED_BOX_THROUGH_WALLS else SboRenderLayers.FILLED_BOX
            val buffer = consumers.getBuffer(renderLayer)

            val minX = -width / 2.0
            val minZ = -depth / 2.0
            val maxX = width / 2.0
            val maxZ = depth / 2.0

            val minY = 0.0
            val maxY = height

            VertexRendering.drawFilledBox(
                this, buffer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                colorComponents[0], colorComponents[1], colorComponents[2], alpha
            )
        }
        //#endif
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
    fun drawString(
        context: WorldRenderContext,
        pos: SboVec,
        yOffset: Double,
        text: String,
        color: Int,
        shadow: Boolean,
        scale: Double,
        throughWalls: Boolean
    ) {
        context.pushPop {
            val camera = context.getCamera()
            val cameraPos = camera.pos
            val cameraYaw = camera.yaw
            val cameraPitch = camera.pitch
            val textRenderer = mc.textRenderer

            val textWorldPos = Vec3d(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
            val distance = cameraPos.distanceTo(textWorldPos)
            val dynamicScale = max(distance, 2.5) * scale

            translate(pos.x + 0.5 - cameraPos.x, pos.y + yOffset + - cameraPos.y, pos.z + 0.5 - cameraPos.z)

            multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw))
            multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch))

            scale(-dynamicScale.toFloat(), -dynamicScale.toFloat(), dynamicScale.toFloat())

            val textWidth = textRenderer.getWidth(text)
            val xOffset = -textWidth / 2f

            val consumers = context.consumers()!!

            val layerType = if (throughWalls) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL

            textRenderer.draw(
                text,
                xOffset,
                0f,
                color,
                shadow,
                peek().positionMatrix,
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
     * @param throughWalls Whether the line should be drawn through walls.
     * @param alpha The alpha value for transparency (0.0 to 1.0).
     */
    fun drawLineFromCursor(
        context: WorldRenderContext,
        target: SboVec,
        color: FloatArray,
        lineWidth: Float,
        throughWalls: Boolean,
        alpha: Float = 0.5f
    ) {
        context.pushPop {
            val camera = context.getCamera()
            val cameraPos = camera.pos

            translate(cameraPos.negate())

            val consumers = context.consumers()!!
            val startPos = cameraPos.add(Vec3d.fromPolar(camera.pitch, camera.yaw))
            val endPos = target.center().toVec3d().add(0.0, 0.5, 0.0)

            val lineDir = endPos.subtract(startPos)
            val viewDir = startPos.subtract(cameraPos)

            val sideVec = lineDir.crossProduct(viewDir).normalize()

            val upVec = sideVec.crossProduct(lineDir).normalize()

            val nx = upVec.x.toFloat()
            val ny = upVec.y.toFloat()
            val nz = upVec.z.toFloat()

            withLineWidth(lineWidth) {
                val renderLayer = if (throughWalls) SboRenderLayers.LINES_THROUGH_WALLS else SboRenderLayers.LINES
                val buffer = consumers.getBuffer(renderLayer)
                val matrixEntry = peek()

                buffer.vertex(matrixEntry, startPos.x.toFloat(), startPos.y.toFloat(), startPos.z.toFloat())
                    .normal(matrixEntry, nx, ny, nz)
                    .color(color[0], color[1], color[2], alpha)
                    //#if MC > 1.21.10
                    //$$ .lineWidth(lineWidth)
                    //#endif


                buffer.vertex(matrixEntry, endPos.x.toFloat(), endPos.y.toFloat(), endPos.z.toFloat())
                    .normal(matrixEntry, nx, ny, nz)
                    .color(color[0], color[1], color[2], alpha)
                    //#if MC > 1.21.10
                    //$$ .lineWidth(lineWidth)
                    //#endif
            }
        }
    }
    /**
     * Renders a beacon beam at the given location.
     * @param context The world render context.
     * @param vec The position in the world where the beacon beam should be rendered.
     * @param colorComponents The RGB color components as a FloatArray (0.0 to
     * @param phase Whether the beam should render through walls.
     */
    @JvmOverloads
    fun renderBeaconBeam(
        ctx: WorldRenderContext,
        vec: SboVec,
        colorComponents: FloatArray,
        phase: Boolean = false
    ) {
        val player = mc.player ?: return
        if (vec.center().distanceTo(player.x, player.y, player.z) < Diana.removeBeam) return

        val consumers = ctx.consumers()
        val wolrd = mc.world ?: return
        val partialTicks = mc.renderTickCounter.getTickProgress(true)
        val cam = ctx.getCamera().pos
        val beamColor = floatArrayOf(colorComponents[0], colorComponents[1], colorComponents[2], 1.0f)

        ctx.pushPop {
            translate(vec.x - cam.x, (vec.y + 1.0) - cam.y, vec.z - cam.z)

            renderBeam(
                consumers!!,
                partialTicks,
                wolrd.time,
                Color(beamColor[0], beamColor[1], beamColor[2]).rgb,
                phase
            )
        }
    }

    private fun MatrixStack.renderBeam(
        vertices: VertexConsumerProvider,
        partialTicks: Float,
        worldTime: Long,
        color: Int,
        phase: Boolean = false
    ) {
        val opaqueLayyer = if (phase) SboRenderLayers.BEACON_BEAM_OPAQUE_THROUGH_WALLS else SboRenderLayers.BEACON_BEAM_OPAQUE
        val transluscentLayer = if (phase) SboRenderLayers.BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS else SboRenderLayers.BEACON_BEAM_TRANSLUCENT
        val heightScale = 1f
        val height = 320
        val innerRadius = 0.2f
        val outerRadius = 0.25f
        val time = Math.floorMod(worldTime, 40) + partialTicks
        val fixedTime = -time
        val wavePhase = MathHelper.fractionalPart(fixedTime * 0.2f - MathHelper.floor(fixedTime * 0.1f).toFloat())
        val animationStep = -1f + wavePhase
        var renderYOffest = height.toFloat() * heightScale * (0.5f / innerRadius) + animationStep

        pushPop {
            translate(0.5, 0.0, 0.5)

            pushPop {
                multiply(RotationAxis.POSITIVE_Y.rotationDegrees(time * 2.25f - 45f))

                renderBeamLayer(
                    vertices.getBuffer(opaqueLayyer),
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
                vertices.getBuffer(transluscentLayer),
                ColorHelper.withAlpha(32, color),
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

    private fun MatrixStack.renderBeamLayer(
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
        val entry = peek()
        renderBeamFace(entry, vertices, color, x1, z1, x2, z2, v1, v2)
        renderBeamFace(entry, vertices, color, x4, z4, x3, z3, v1, v2)
        renderBeamFace(entry, vertices, color, x2, z2, x4, z4, v1, v2)
        renderBeamFace(entry, vertices, color, x3, z3, x1, z1, v1, v2)
    }

    private fun renderBeamFace(
        matrix: MatrixStack.Entry,
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
        matrix: MatrixStack.Entry,
        vertices: VertexConsumer,
        color: Int,
        y: Int,
        x: Float,
        z: Float,
        u: Float,
        v: Float
    ) {
        vertices
            .vertex(matrix, x, y.toFloat(), z)
            .color(color)
            .texture(u, v)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(15728880).normal(matrix, 0f, 1f, 0f)
    }

    private fun WorldRenderContext.getCamera(): Camera {
        return gameRenderer().camera
    }

    private inline fun WorldRenderContext.pushPop(function: MatrixStack.() -> Unit) {
        val matrix = matrices()!!
        matrix.pushPop(function)
    }

    private inline fun MatrixStack.withLineWidth(lineWidth: Float, function: MatrixStack.() -> Unit) {
        //#if MC > 1.21.10
        //$$ function()
        //#else
        val prevLineWidth = RenderSystem.getShaderLineWidth()
        RenderSystem.lineWidth(lineWidth)
        function()
        RenderSystem.lineWidth(prevLineWidth)
        //#endif
    }

    private inline fun MatrixStack.pushPop(function: MatrixStack.() -> Unit) {
        this.push()
        function()
        this.pop()
    }
}
