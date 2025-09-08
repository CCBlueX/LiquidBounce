package net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.modes

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.BreadcrumbsMode
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.ModuleBreadcrumbs.colorMode
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
object SparkleMode : BreadcrumbsMode("Sparkle") {

    private val alive by int("Alive", 30, 2..3000, "ticks")
    private val fadeFactor by float("FadeSpeed",0.2f,0.1f..1f)

    private val path = ArrayDeque<Sparkle>()
    private data class Sparkle(
        val pos: Vec3d,
        val creationTime: Long
    )

    private val updateHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        val currentTime = mc.world?.time ?: return@handler

        val currPos = Vec3d(player.x, player.y, player.z)
        val lastPos = path.peekLast()?.pos

        if (lastPos == null || lastPos.x != currPos.x || lastPos.y != currPos.y || lastPos.z != currPos.z) {
            path.add(Sparkle(currPos, currentTime))
        }

        while (path.isNotEmpty() && currentTime - path.peekFirst().creationTime > alive) {
            path.removeFirst()
        }
    }

    private val worldChangeHandler = handler<WorldChangeEvent> {
        path.clear()
    }

    private val glowTexture = "particles/glow.png".registerAsDynamicImageFromClientResources()

    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (path.isEmpty()) return@handler
        val currentTime = mc.world?.time ?: return@handler
        val matrixStack = event.matrixStack
        renderEnvironmentForWorld(matrixStack) {
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                GlStateManager.DstFactor.ONE,
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE
            )
            RenderSystem.disableCull()
            RenderSystem.disableDepthTest()
            RenderSystem.setShaderTexture(0, glowTexture)
            val camera = mc.gameRenderer.camera
            val (color1Base, color2Base) = colorMode.activeChoice.getColors(player)
            val time = mc.world?.time?.toFloat() ?: 0f
            var index = 0
            path.forEach { point ->
                index++
                val pos = point.pos
                val x = pos.x - camera.pos.x
                val y = pos.y - camera.pos.y
                val z = pos.z - camera.pos.z
                val distance = player.pos.squaredDistanceTo(pos.x, pos.y - 1.0, pos.z).let { sqrt(it) }
                if (index % 10 != 0 && distance > 25) return@forEach
                if (index % 3 == 0 && distance > 15) return@forEach
                val progress = 1f - ((currentTime - point.creationTime).toFloat() / alive.toFloat()).coerceIn(0f, 1f)
                matrixStack.push()
                matrixStack.translate(x, y, z)
                val size = 0.3f + 0.2f * (1f - progress)
                matrixStack.scale(size, size, size)
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.yaw))
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))

                val t = (sin((time * fadeFactor) + -(index / path.size.toFloat()) * 13) + 1) * 0.5f
                val color = color1Base.blend(color2Base, t)
                val alpha = (color.a * progress).toInt()
                val renderColor = color.withAlpha(alpha)

                drawCustomMesh(
                    DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    ShaderProgramKeys.POSITION_TEX_COLOR
                ) { mat ->
                    vertex(mat, -0.5f, -0.5f, 0f)
                        .texture(0f, 0f)
                        .color(renderColor.toARGB())
                    vertex(mat,  0.5f, -0.5f, 0f)
                        .texture(1f, 0f)
                        .color(renderColor.toARGB())
                    vertex(mat,  0.5f,  0.5f, 0f)
                        .texture(1f, 1f)
                        .color(renderColor.toARGB())
                    vertex(mat, -0.5f,  0.5f, 0f)
                        .texture(0f, 1f)
                        .color(renderColor.toARGB())
                }
                matrixStack.pop()
            }
        }
    }


    override fun enable() {
        path.clear()
    }

    override fun disable() {
        path.clear()
    }
}
