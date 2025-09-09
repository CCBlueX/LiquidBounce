@file:Suppress("unused")
package net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.modes

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
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
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.*

object SparkleMode : BreadcrumbsMode("Sparkle") {
    private val alive by int("Alive", 30, 2..3000, "ticks")
    private val gradientSpeed by float("GradientSpeed",0.2f,0.1f..1f)

    private object Animation :ToggleableConfigurable(this,"Animation", true) {
        val fade by boolean("Fade",false)
        val shrink by boolean("Shrink",true)
        val fadeDuration by float("FadeDuration",5f,4f..10f,"tick")
        val shrinkDuration by float("ShrinkDuration",5f,4f..10f,"tick")
    }

    private object DistanceLimit :ToggleableConfigurable(this,"DistanceLimit", false) {
         val distanceRange by floatRange("DistanceLimit", 256f..256f, 16f..256f)
    }

    init {
        tree(Animation)
        tree(DistanceLimit)
    }

    private val darkImprint by boolean("DarkImprint", true)

    private val path = ArrayDeque<Sparkle>()

    private data class Sparkle(
        val pos: Vec3d,
        val creationTime: Long
    )
    private val glowTexture = "particles/glow.png".registerAsDynamicImageFromClientResources()

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


    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (path.isEmpty()) return@handler
        val currentTime = mc.world?.time ?: return@handler
        val matrixStack = event.matrixStack
        renderEnvironmentForWorld(matrixStack) {
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.disableDepthTest()
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                if (darkImprint) {
                    GlStateManager.DstFactor.ONE
                } else {
                    GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
                },
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE
            )

            RenderSystem.setShaderTexture(0, glowTexture)
            val camera = mc.gameRenderer.camera
            val (color1Base, color2Base) = colorMode.activeChoice.getColors(player)
            val time = mc.world?.time?.toFloat() ?: 0f

            val total = path.size
            var index = 0

            val yawRad = Math.toRadians(camera.yaw.toDouble())
            val pitchRad = Math.toRadians(camera.pitch.toDouble())
            val forwardX = -sin(yawRad) * cos(pitchRad)
            val forwardY = -sin(pitchRad)
            val forwardZ = cos(yawRad) * cos(pitchRad)

            var fLen = sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ)
            val fx = if (fLen != 0.0) forwardX / fLen else 0.0
            val fy = if (fLen != 0.0) forwardY / fLen else 0.0
            val fz = if (fLen != 0.0) forwardZ / fLen else 1.0

            val uxw = 0.0
            val uyw = 1.0
            val uzw = 0.0

            var rightX = uyw * fz - uzw * fy
            var rightY = uzw * fx - uxw * fz
            var rightZ = uxw * fy - uyw * fx
            var rLen = sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ)
            if (rLen == 0.0) {
                rightX = 1.0; rightY = 0.0; rightZ = 0.0; rLen = 1.0
            } else {
                rightX /= rLen; rightY /= rLen; rightZ /= rLen
            }

            var upX = fy * rightZ - fz * rightY
            var upY = fz * rightX - fx * rightZ
            var upZ = fx * rightY - fy * rightX
            var uLen = sqrt(upX * upX + upY * upY + upZ * upZ)
            if (uLen == 0.0) {
                upX = 0.0; upY = 1.0; upZ = 0.0
            } else {
                upX /= uLen; upY /= uLen; upZ /= uLen
            }
            val maxDistSq1 = DistanceLimit.distanceRange.endInclusive * DistanceLimit.distanceRange.endInclusive
            val maxDistSq2 = DistanceLimit.distanceRange.start * DistanceLimit.distanceRange.start

            drawCustomMesh(
                DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { mat ->
                val iter2 = path.iterator()
                while (iter2.hasNext()) {
                    index++
                    val point = iter2.next()
                    val pos = point.pos
                    val cx = pos.x - camera.pos.x
                    val cy = pos.y - camera.pos.y
                    val cz = pos.z - camera.pos.z

                    val distSq = player.squaredDistanceTo(pos.x, pos.y - 1.0, pos.z)

                    if (DistanceLimit.enabled) {
                        if (index % 10 != 0 && distSq > maxDistSq1) continue
                        if (index % 3 == 0 && distSq > maxDistSq2) continue
                    }

                    val life = (currentTime - point.creationTime).toFloat()
                    val totalLife = alive.toFloat()
                    val progress = if (Animation.enabled && Animation.fade) {
                        1f - (life / totalLife).coerceIn(0f, 1f)
                    } else 1f

                    val fadeFactor = if (Animation.enabled && Animation.fade) {
                        val fadeStart = totalLife - Animation.fadeDuration
                        if (life >= fadeStart) {
                            val fadeProgress = ((life - fadeStart) / Animation.fadeDuration).coerceIn(0f, 1f)
                            1f - fadeProgress
                        } else 1f
                    } else 1f
                    val shrinkFactor = if (Animation.enabled && Animation.shrink && life >= totalLife - Animation.shrinkDuration) {
                        val shrinkProgress = ((life - (totalLife - Animation.shrinkDuration)) / Animation.shrinkDuration).coerceIn(0f, 1f)
                        1f - shrinkProgress
                    } else 1f

                    val baseSize = 0.5f
                    val size = (baseSize * shrinkFactor) * (0.5f + 0.5f * progress)
                    val half = size * 0.5f

                    val t = (sin((time * gradientSpeed) + -(index / total.toFloat()) * 13) + 1) * 0.5f
                    val color = color1Base.blend(color2Base, t)
                    val alpha = (color.a * progress * fadeFactor).toInt()
                    val renderColor = color.withAlpha(alpha)
                    val argb = renderColor.toARGB()

                    val rx = (rightX * half).toFloat()
                    val ry = (rightY * half).toFloat()
                    val rz = (rightZ * half).toFloat()
                    val ux2 = (upX * half).toFloat()
                    val uy2 = (upY * half).toFloat()
                    val uz2 = (upZ * half).toFloat()

                    val v0x = (cx - rx - ux2).toFloat()
                    val v0y = (cy - ry - uy2).toFloat()
                    val v0z = (cz - rz - uz2).toFloat()
                    val v1x = (cx + rx - ux2).toFloat()
                    val v1y = (cy + ry - uy2).toFloat()
                    val v1z = (cz + rz - uz2).toFloat()
                    val v2x = (cx + rx + ux2).toFloat()
                    val v2y = (cy + ry + uy2).toFloat()
                    val v2z = (cz + rz + uz2).toFloat()
                    val v3x = (cx - rx + ux2).toFloat()
                    val v3y = (cy - ry + uy2).toFloat()
                    val v3z = (cz - rz + uz2).toFloat()

                    vertex(mat, v0x, v0y, v0z).texture(0f, 1f).color(argb)
                    vertex(mat, v1x, v1y, v1z).texture(1f, 1f).color(argb)
                    vertex(mat, v2x, v2y, v2z).texture(1f, 0f).color(argb)
                    vertex(mat, v3x, v3y, v3z).texture(0f, 0f).color(argb)
                }
            }
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(true)
        }
    }


    override fun enable() {
        path.clear()
    }

    override fun disable() {
        path.clear()
    }
}
