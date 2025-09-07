package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Color4b.Companion.hslToRgb
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.option.Perspective
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.math.RotationAxis
import org.lwjgl.opengl.GL11
import kotlin.math.cos
import kotlin.math.sin

object ModuleChinaHat : ClientModule("ChinaHat", Category.RENDER, aliases = arrayOf("BambooHat")) {
    private val onlySelf by boolean("OnlySelf", true)
    private val side by int("Side", 16, 16..256)
    private val alpha by int("Alpha", 100, 0..255)
    private val height by float("Height",0.2f,0.0f..0.5f)
    private val offsetY by float("OffsetY",0.1f,0.0f..0.35f)
    private val rotation by float("RotationX", 0f, -180f..180f)
    private val gradientSpeed by float("GradientSpeed", 1f,0.2f..2f)
    private val colorMode = choices(this, "ColorMode") {
        arrayOf(
            GenericStaticColorMode(it, Color4b.CYAN),
            GenericRainbowColorMode(it),
        )
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val world = mc.world ?: return@handler
        var players = if (!onlySelf) world.players else listOfNotNull(mc.player)

        if (mc.player != null) {
            if (mc.options.perspective == Perspective.FIRST_PERSON && players.contains(mc.player)) {
                players -= mc.player
            }
        }

        renderEnvironmentForWorld(event.matrixStack) {
            players.forEach { player ->
                if (player.isSpectator || !player.isAlive) return@forEach

                val interp = player.interpolateCurrentPosition(event.partialTicks)
                val yOffset = player.standingEyeHeight + offsetY

                withPositionRelativeToCamera(interp.add(0.0, yOffset.toDouble(), 0.0)) {
                    val ms = event.matrixStack
                    ms.push()
                    ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-player.yaw))
                    ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees( rotation))
                    RenderSystem.disableCull()

                    GL11.glEnable(GL11.GL_BLEND)
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                    GL11.glDepthMask(false)


                    GL11.glLineWidth(1.0f)

                    val matrix = ms.peek().positionMatrix
                    val tessellator = RenderSystem.renderThreadTesselator()
                    val buffer = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR)
                    RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)

                    val topRadius = 0.8f
                    val height = height
                    val sides = side
                    val currentTime = System.currentTimeMillis()
                    val animationDuration = (4000f / gradientSpeed).coerceAtLeast(1f)

                    for (j in 0 until sides) {
                        val jNext = (j + 1) % sides
                        val theta = 2.0 * Math.PI * j / sides
                        val thetaNext = 2.0 * Math.PI * jNext / sides

                        val x1 = (cos(theta) * topRadius).toFloat()
                        val z1 = (sin(theta) * topRadius).toFloat()
                        val x1n = (cos(thetaNext) * topRadius).toFloat()
                        val z1n = (sin(thetaNext) * topRadius).toFloat()

                        val y1 = 0f
                        val y2 = height

                        val baseProgress = (System.currentTimeMillis() % animationDuration.toLong()) / animationDuration

                        val progressOuter1 = (baseProgress + j / sides.toFloat()) % 1f
                        val progressOuter2 = (baseProgress + jNext / sides.toFloat()) % 1f
                        val progressTop = (baseProgress + (j + 0.5f) / sides.toFloat()) % 1f

                        val colorOuter1 = when (val mode = colorMode.activeChoice) {
                            is GenericRainbowColorMode -> hslToRgb(progressOuter1, 0.95f, 0.65f, alpha)
                            else -> mode.getColors(mc.player).let { (c1, c2) -> c1.blend(c2, progressOuter1).withAlpha(alpha) }
                        }

                        val colorOuter2 = when (val mode = colorMode.activeChoice) {
                            is GenericRainbowColorMode -> hslToRgb(progressOuter2, 0.95f, 0.65f, alpha)
                            else -> mode.getColors(mc.player).let { (c1, c2) -> c1.blend(c2, progressOuter2).withAlpha(alpha) }
                        }

                        val colorTop = when (val mode = colorMode.activeChoice) {
                            is GenericRainbowColorMode -> hslToRgb(progressTop, 0.95f, 0.65f, alpha)
                            else -> mode.getColors(mc.player).let { (c1, c2) -> c1.blend(c2, progressTop).withAlpha(alpha) }
                        }

                        buffer.vertex(matrix, x1, y1, z1)
                            .color(colorOuter1.r / 255f, colorOuter1.g / 255f, colorOuter1.b / 255f, colorOuter1.a / 255f)
                        buffer.vertex(matrix, 0f, y2, 0f)
                            .color(colorTop.r / 255f, colorTop.g / 255f, colorTop.b / 255f, colorTop.a / 255f)
                        buffer.vertex(matrix, x1n, y1, z1n)
                            .color(colorOuter2.r / 255f, colorOuter2.g / 255f, colorOuter2.b / 255f, colorOuter2.a / 255f)

                    }

                    BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return@forEach)
                    GL11.glDepthMask(true)
                    ms.pop()

                    RenderSystem.enableCull()
                }
            }
        }
    }


}
