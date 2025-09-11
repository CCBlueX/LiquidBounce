@file:Suppress("unused")
package net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.modes

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.BreadcrumbsMode
import net.ccbluex.liquidbounce.features.module.modules.render.breadcrumbs.ModuleBreadcrumbs.colorMode
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.*

object SparkleMode : BreadcrumbsMode("Sparkle") {

    private val alive by int("Alive", 20, 2..3000, "ticks")
    private val size by float("Size",0.33f,0.1f..1.0f)
    private val gradientSpeed by int("GradientSpeed",5,1..10)

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
    private val onlyOwn by boolean("OnlyOwn", true)

    private val trails = IdentityHashMap<Entity, Trail>()
    private val lastPositions = IdentityHashMap<Entity, DoubleArray>()

    private class Trail {
        val positions = ArrayDeque<Sparkle>()
    }
    private data class Sparkle(
        val pos: Vec3d,
        val creationTime: Long
    )

    private val glowTexture = "particles/glow.png".registerAsDynamicImageFromClientResources()

    private val debugUpdateHandler = handler<GameTickEvent> {
        trails.forEach { (entity, trail) ->
            debugParameter(this@SparkleMode, "Entity ${entity.id} TrailSize", trail.positions.size)
        }
    }

    private val updateHandler = handler<GameTickEvent> {
        val time = mc.world?.time ?: return@handler
        val entities =if (onlyOwn) listOf(player) else mc.world?.players ?: return@handler

        entities.forEach { entity ->
            val last = lastPositions[entity]
            if (last == null || entity.x != last[0] || entity.y != last[1] || entity.z != last[2]) {
                lastPositions[entity] = doubleArrayOf(entity.x, entity.y, entity.z)
                trails.getOrPut(entity, ::Trail).positions.add(Sparkle(Vec3d(entity.x, entity.y, entity.z), time))
            }
        }

        trails.forEach { (_, trail) ->
            while (trail.positions.isNotEmpty() && time - trail.positions.peekFirst().creationTime > alive) {
                trail.positions.removeFirst()
            }
        }

        trails.keys.retainAll { entity -> entity.isAlive && (!onlyOwn || entity === player) }
    }


    private val worldChangeHandler = handler<WorldChangeEvent> {
        trails.clear()
    }

    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (trails.isEmpty()) return@handler
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
            val clientTime = System.nanoTime() / 1_000_000_000.0
            var index = 0

            val yawRad = Math.toRadians(camera.yaw.toDouble())
            val pitchRad = Math.toRadians(camera.pitch.toDouble())
            val forwardX = -sin(yawRad) * cos(pitchRad)
            val forwardY = -sin(pitchRad)
            val forwardZ = cos(yawRad) * cos(pitchRad)

            val forward = Vec3d(
                -sin(yawRad) * cos(pitchRad),
                -sin(pitchRad),
                cos(yawRad) * cos(pitchRad)
            ).normalize()

            val up = Vec3d(0.0, 1.0, 0.0)
            val right = forward.crossProduct(up).normalize()
            val upAdjusted = right.crossProduct(forward).normalize()


            val uxw = 0.0
            val uyw = 1.0
            val uzw = 0.0

            var rightX = uyw * forward.z - uzw * forward.y
            var rightY = uzw * forward.x - uxw * forward.z
            var rightZ = uxw * forward.y - uyw * forward.x
            var rLen = sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ)
            if (rLen == 0.0) {
                rightX = 1.0; rightY = 0.0; rightZ = 0.0; rLen = 1.0
            } else {
                rightX /= rLen; rightY /= rLen; rightZ /= rLen
            }

            var upX = forward.y * rightZ - forward.z * rightY
            var upY = forward.z * rightX - forward.x * rightZ
            var upZ = forward.x * rightY - forward.y * rightX
            var uLen = sqrt(upX * upX + upY * upY + upZ * upZ)
            if (uLen == 0.0) {
                upX = 0.0; upY = 1.0; upZ = 0.0
            } else {
                upX /= uLen; upY /= uLen; upZ /= uLen
            }
            val maxDistSq1 = DistanceLimit.distanceRange.endInclusive * DistanceLimit.distanceRange.endInclusive
            val maxDistSq2 = DistanceLimit.distanceRange.start * DistanceLimit.distanceRange.start


            trails.forEach { (entity, trail) ->
                if (onlyOwn && entity !== player) return@forEach
                val total = trail.positions.size
                debugParameter(this@SparkleMode, "Entity ${entity.id} TrailSizeRender", trail.positions.size)
                drawCustomMesh(
                    DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    ShaderProgramKeys.POSITION_TEX_COLOR
                ) { mat ->
                    var index = 0
                    trail.positions.forEach { sparkle ->
                        index++
                        val pos = sparkle.pos
                        val cx = pos.x - camera.pos.x
                        val cy = pos.y - camera.pos.y
                        val cz = pos.z - camera.pos.z

                        val distSq = player.squaredDistanceTo(pos.x, pos.y - 1.0, pos.z)
                        if (DistanceLimit.enabled) {
                            if (index % 10 != 0 && distSq > maxDistSq1) return@forEach
                            if (index % 3 == 0 && distSq > maxDistSq2) return@forEach
                        }

                        val life = (currentTime - sparkle.creationTime).toFloat()
                        val totalLife = alive.toFloat()
                        val progress = if (Animation.enabled && Animation.fade) {
                            1f - (life / totalLife).coerceIn(0f, 1f)
                        } else {
                            1f
                        }

                        val fadeFactor = if (Animation.enabled && Animation.fade) {
                            val fadeStart = totalLife - Animation.fadeDuration
                            if (life >= fadeStart) {
                                val fadeProgress = ((life - fadeStart) / Animation.fadeDuration).coerceIn(0f, 1f)
                                1f - fadeProgress
                            } else {
                                1f
                            }
                        } else {
                            1f
                        }
                        val shrinkFactor = if (Animation.enabled && Animation.shrink
                            && life >= totalLife - Animation.shrinkDuration
                        ) {
                            val shrinkProgress = ((life - (
                                totalLife - Animation.shrinkDuration)) / Animation.shrinkDuration).coerceIn(0f, 1f)
                            1f - shrinkProgress
                        } else {
                            1f
                        }


                        val size = (size * shrinkFactor) * (0.5f + 0.5f * progress)
                        val half = size * 0.5f

                        val t = (sin((clientTime * gradientSpeed) + -(index / total.toFloat()) * 13) + 1) * 0.5f
                        val color = color1Base.blend(color2Base, t.toFloat())
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
            }
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(true)
        }
    }
    override fun enable() {
        trails.clear()
    }

    override fun disable() {
        trails.clear()
    }
}
