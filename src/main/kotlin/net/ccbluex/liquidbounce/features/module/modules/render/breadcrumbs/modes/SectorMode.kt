package net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.modes

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.BreadcrumbsMode
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.ModuleBreadcrumbs.colorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
object SectorMode : BreadcrumbsMode("Sector") {

    private val alive by int("Alive", 7, 2..100, "ticks")
    private val trails = IdentityHashMap<Entity, Trail>()
    private val lastPositions = IdentityHashMap<Entity, DoubleArray>()

    private class Trail {
        val positions = ArrayDeque<TrailPoint>()
    }
    private data class TrailPoint(
        val pos: Vec3d,
        val creationTime: Long
    )


    private val updateHandler = handler<GameTickEvent> {
        val time = mc.world?.time ?: return@handler

        val last = lastPositions[player]
        if (last == null || player.x != last[0] || player.y != last[1] || player.z != last[2]) {
            lastPositions[player] = doubleArrayOf(player.x, player.y, player.z)
            trails.getOrPut(player, ::Trail).positions.add(
                TrailPoint(Vec3d(player.x, player.y, player.z), time)
            )
        }

        trails.keys.retainAll { it === player && it.isAlive }
    }


    private val worldChangeHandler = handler<WorldChangeEvent> {
        trails.clear()
    }

    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (trails.isEmpty()) return@handler
        val currentTime = System.nanoTime() / 1_000_000_000.0
        val matrixStack = event.matrixStack
        val camera = mc.gameRenderer.camera
        val (color1Base, color2Base) = colorMode.activeChoice.getColors(player)

        renderEnvironmentForWorld(matrixStack) {
            RenderSystem.enableBlend()
            RenderSystem.disableCull()
            RenderSystem.disableDepthTest()
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

            val tessellator = RenderSystem.renderThreadTesselator()
            val buffer = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR)

            trails.forEach { (entity, trail) ->
                trail.positions.removeIf { currentTime - it.creationTime > alive }
                if (trail.positions.isEmpty()) return@forEach

                var index = 0
                trail.positions.forEach { point ->
                    index++
                    val pos = point.pos
                    val x = pos.x - camera.pos.x
                    val y = pos.y - camera.pos.y
                    val z = pos.z - camera.pos.z
                    val distance = player.pos.squaredDistanceTo(pos.x, pos.y - 1.0, pos.z).let { sqrt(it) }
                    if (index % 10 != 0 && distance > 25) return@forEach
                    if (index % 3 == 0 && distance > 15) return@forEach

                    val quality = (distance * 4 + 10).toInt().coerceAtMost(350)
                    val progress = 1f - ((currentTime - point.creationTime).toFloat() / alive.toFloat()).coerceIn(0f, 1f)
                    if (progress <= 0f) return@forEach

                    matrixStack.push()
                    matrixStack.translate(x, y, z)
                    matrixStack.scale(-0.04f, -0.04f, -0.04f)
                    matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.yaw))
                    matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))

                    val t = (sin((currentTime * 2f) + -(index / trail.positions.size.toFloat()) * 13) + 1)
                    val color = color1Base.blend(color2Base, t.toFloat())
                    val alpha = (color.a * progress).toInt()
                    val matrix = Matrix4f(matrixStack.peek().positionMatrix)

                    fun addCircle(radius: Float, col: Color4b, q: Int) {
                        val argb = col.toARGB()
                        buffer.vertex(matrix, 0f, 0f, 0f).color(argb)
                        val step = (2.0 * Math.PI / q)
                        for (i in 0..q) {
                            val angle = i * step
                            val vx = (cos(angle) * radius).toFloat()
                            val vy = (sin(angle) * radius).toFloat()
                            buffer.vertex(matrix, vx, vy, 0f).color(argb)
                        }
                    }

                    addCircle(0.7f, color.withAlpha(alpha), quality)
                    if (distance < 4) addCircle(1.4f, color.withAlpha((alpha * 0.5f).toInt()), quality)
                    if (distance < 20) addCircle(2.3f, color.withAlpha((alpha * 0.3f).toInt()), quality)

                    matrixStack.pop()
                }
            }

            BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return@renderEnvironmentForWorld)
        }
    }


    override fun enable() {
        trails.clear()
    }

    override fun disable() {
        trails.clear()
    }
}
