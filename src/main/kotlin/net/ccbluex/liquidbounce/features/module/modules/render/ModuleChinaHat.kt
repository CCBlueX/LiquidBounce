package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.config.types.Choice
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import org.joml.Matrix4f
import org.joml.Vector4f
import kotlin.math.*

/**
 * ChinaHat module.
 * Draws a "ChinaHat" (cone) above players' heads.
 * @author errorverybadcode
 */
object ModuleChinaHat : ClientModule("ChinaHat", Category.RENDER) {
    private val onlyOwn by boolean("OnlyOwn", true)
    private val height by float("Height", 0.3f, 0f..2f)
    private val radius by float("Radius", 0.5f, 0.1f..2f)
    private val segments by int("Segments", 128, 16..128)
    private val yOffset by float("YOffset", 0f, -1f..1f)
    private val colorMode = choices("ColorMode", Gradient, arrayOf(Static, Gradient, Rainbow))

    object Static : Choice("Static") {
        override val parent get() = colorMode
        val color by color("Color", Color4b(70, 119, 255, 120))
    }

    object Gradient : Choice("Gradient") {
        override val parent get() = colorMode
        val colorStart by color("StartColor", Color4b(0, 0, 255, 0))
        val colorEnd by color("EndColor", Color4b(105, 222, 255, 255))
        val speed by float("RotationSpeed", 1f, 0.1f..10f)
    }

    object Rainbow : Choice("Rainbow") {
        override val parent get() = colorMode
    }

    override fun disable() {}

    val renderHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            drawHats(event.matrixStack, colorMode.activeChoice)
        }
    }

    private fun drawHats(matrixStack: MatrixStack, mode: Choice) {
        val camera = mc.entityRenderDispatcher.camera ?: return
        val isFirstPerson = mc.options.perspective.isFirstPerson && mc.cameraEntity === player
        val players = if (onlyOwn) listOf(player) else world.players

        RenderSystem.enableDepthTest()
        RenderSystem.disableCull()
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.depthMask(false)

        for (p in players) {
            if (p === player && isFirstPerson) continue
            val buffer = RenderSystem.renderThreadTesselator().begin(
                VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR
            )
            drawHat(matrixStack.peek().positionMatrix, buffer, camera, p, mode)
            BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: continue)
        }

        RenderSystem.depthMask(true)
        RenderSystem.disableBlend()
        RenderSystem.enableCull()
    }

    private fun drawHat(
        matrix: Matrix4f,
        buffer: BufferBuilder,
        camera: Camera,
        entity: Entity,
        mode: Choice
    ) {
        val tickDelta = mc.renderTickCounter.getTickDelta(true)
        val px = ((entity.prevX + (entity.x - entity.prevX) * tickDelta) - camera.pos.x).toFloat()
        val py = ((entity.prevY + (entity.y - entity.prevY) * tickDelta) - camera.pos.y + entity.height + yOffset).toFloat()
        val pz = ((entity.prevZ + (entity.z - entity.prevZ) * tickDelta) - camera.pos.z).toFloat()

        val segs = segments
        val r = radius
        val h = height

        val staticAlpha = (Static.color.a / 255f)
        val time = (System.currentTimeMillis() % 360000L) / 1000.0
        val gradientRotation = if (mode is Gradient) time * mode.speed * Math.PI else 0.0

        val colorFun: (Float, Int) -> Vector4f = when (mode) {
            is Static -> { _, _ -> mode.color.toVec4f() }
            is Gradient -> { t, i ->
                if (t == 0f) mode.colorStart.toVec4f()
                else {
                    val angle = 2.0 * Math.PI * i / segs + gradientRotation
                    val factor = (sin(angle) * 0.5 + 0.5).toFloat()
                    lerpColor(mode.colorStart, mode.colorEnd, factor).toVec4f()
                }
            }
            is Rainbow -> { _, _ -> rainbow().toVec4f().apply { w = staticAlpha } }
            else -> { _, _ -> Vector4f(70 / 255f, 119 / 255f, 255 / 255f, 120 / 255f) }
        }

        val centerCol = colorFun(0f, 0)
        buffer.vertex(matrix, px, py + h, pz).color(centerCol.x, centerCol.y, centerCol.z, centerCol.w)
        for (i in 0..segs) {
            val angle = 2.0 * Math.PI * i / segs
            val dx = (cos(angle) * r).toFloat()
            val dz = (sin(angle) * r).toFloat()
            val col = colorFun(1f, i)
            buffer.vertex(matrix, px + dx, py, pz + dz).color(col.x, col.y, col.z, col.w)
        }
    }

    private fun lerpColor(a: Color4b, b: Color4b, t: Float): Color4b =
        Color4b(
            lerp(a.r, b.r, t),
            lerp(a.g, b.g, t),
            lerp(a.b, b.b, t),
            lerp(a.a, b.a, t)
        )

    private fun lerp(a: Int, b: Int, t: Float): Int =
        (a + ((b - a) * t)).toInt().coerceIn(0, 255)

    private fun Color4b.toVec4f() =
        Vector4f(r / 255f, g / 255f, b / 255f, a / 255f)
}
