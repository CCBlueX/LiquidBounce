/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawGradientCircle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.shiftHue
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.math.interpolate
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.joml.Quaternionf
import kotlin.math.max

object ModuleJumpEffect : ClientModule("JumpEffect", Category.RENDER) {

    private val endRadius by floatRange("EndRadius", 0.15F..0.8F, 0F..3F)
    val innerColor by color("InnerColor", Color4b(0, 255, 4, 0))
    val outerColor by color("OuterColor", Color4b(0, 255, 4, 89))
    private val animCurve by curve("AnimCurve", Easing.QUAD_OUT)
    private val hueOffsetAnim by int("HueOffsetAnim", 63, -360..360)
    private val lifetime by int("Lifetime", 15, 1..30)

    object JumpParticles : ToggleableConfigurable(this, "JumpParticles", false) {
        val particleSize by float("JumpParticleSize", 1f, 0.5f..2f)
        val speed by float("JumpParticleSpeed", 1f, 0.5f..5f)
        val count by intRange("JumpParticleCount", 2..10, 2..30)
        val rotate by boolean("JumpParticleRandomRotation", true)
        val particleImages by multiEnumChoice("Particle", ParticleImage.entries, canBeNone = false)
        val particleLifetime by float("JumpParticleLifetime", 2.5f, 0.5f..5f)
    }

    private val circles = ArrayDeque<ObjectLongMutablePair<Vec3d>>()
    private val particles = ArrayList<JumpParticle>()

    init {
        tree(JumpParticles)
    }

    val repeatable = tickHandler {
        circles.removeIf { pair ->
            val newValue = pair.valueLong() + 1L
            if (newValue >= lifetime) {
                true
            } else {
                pair.value(newValue)
                false
            }
        }
    }

    @Suppress("unused")
    val onJump = handler<PlayerJumpEvent> {
        circles.add(ObjectLongMutablePair(player.pos, 0L))
        if (!JumpParticles.enabled) return@handler

        val belowCircle = player.pos.subtract(0.0, -0.1, 0.0)
        repeat(JumpParticles.count.random()) {
            particles.add(
                JumpParticle(
                    pos = belowCircle,
                    particleImage = JumpParticles.particleImages.random()
                )
            )
        }
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack
        renderEnvironmentForWorld(matrixStack) {
            circles.forEach {
                val progress = animCurve
                    .transform((it.valueLong() + event.partialTicks) / lifetime)
                    .coerceIn(0f..1f)

                withPositionRelativeToCamera(it.key()) {
                    drawGradientCircle(
                        endRadius.endInclusive * progress,
                        endRadius.start * progress,
                        animateColor(outerColor, progress),
                        animateColor(innerColor, progress)
                    )
                }
            }

            if (JumpParticles.enabled) {
                RenderSystem.depthMask(true)
                RenderSystem.disableCull()
                mc.gameRenderer.lightmapTextureManager.disable()
                RenderSystem.defaultBlendFunc()

                particles.removeIf { particle ->
                    val currentTime = System.currentTimeMillis()
                    val ageSeconds = (currentTime - particle.spawnTime) / 1000f
                    val expired = ageSeconds > JumpParticles.particleLifetime
                    val outOfRange = player.pos.distanceTo(particle.pos) > 30
                    val transparent = particle.alpha <= 0f

                    if (!(expired || outOfRange || transparent)) {
                        particle.update(event.partialTicks.toDouble())
                        matrixStack.push()
                        RenderSystem.setShaderTexture(0, particle.particleImage.texture)
                        renderJumpParticle(particle, event.partialTicks)
                        matrixStack.pop()
                    }

                    expired || outOfRange || transparent
                }

                RenderSystem.depthMask(true)
                RenderSystem.enableCull()
                RenderSystem.defaultBlendFunc()
                mc.gameRenderer.lightmapTextureManager.enable()
            }
        }
    }

    private fun animateColor(baseColor: Color4b, progress: Float): Color4b {
        val faded = baseColor.fade(1.0F - progress)
        return if (hueOffsetAnim == 0) faded else shiftHue(faded, (hueOffsetAnim * progress).toInt())
    }

    enum class ParticleImage(
        override val choiceName: String,
        val texture: Identifier
    ) : NamedChoice {
        ORBIZ("Orbiz", "particles/glow.png".registerAsDynamicImageFromClientResources()),
        STAR("Star", "particles/star.png".registerAsDynamicImageFromClientResources()),
        DOLLAR("Dollar", "particles/dollar.png".registerAsDynamicImageFromClientResources())
    }

    private class JumpParticle(
        var pos: Vec3d,
        var prevPos: Vec3d = pos,
        var velocity: Vec3d = Vec3d(
            (-0.01..0.01).random(),
            (0.01..0.02).random(),
            (-0.01..0.01).random()
        ),
        var collisionTime: Long = -1,
        var alpha: Float = 1.0f,
        val spawnTime: Long = System.currentTimeMillis(),
        val rotation: Float = (0f..360f).random(),
        val particleImage: ParticleImage
    ) {
        fun update(delta: Double) {
            val particleSpeed = JumpParticles.speed.toDouble()
            prevPos = pos

            if (collisionTime != -1L) {
                val timeSinceCollision = System.currentTimeMillis() - collisionTime
                alpha = max(0f, 1f - (timeSinceCollision / 3000f))
            }

            velocity = velocity.add(0.0, -0.0001, 0.0)
            val nextPos = pos.add((velocity * delta).multiply(particleSpeed, 1.0, particleSpeed))

            if (!nextPos.isBlockAir) {
                if (collisionTime == -1L) collisionTime = System.currentTimeMillis()
                val dx = velocity.x * delta * particleSpeed
                val dy = velocity.y * delta
                val dz = velocity.z * delta * particleSpeed

                velocity = Vec3d(
                    if (!Vec3d(pos.x + dx, pos.y, pos.z).isBlockAir) 0.0 else velocity.x,
                    if (!Vec3d(pos.x, pos.y + dy, pos.z).isBlockAir) -velocity.y * 0.5 else velocity.y,
                    if (!Vec3d(pos.x, pos.y, pos.z + dz).isBlockAir) 0.0 else velocity.z
                )
                pos = pos.add((velocity * delta).multiply(particleSpeed, 1.0, particleSpeed))
            } else {
                pos = nextPos
            }
        }
    }

    private fun WorldRenderEnvironment.renderJumpParticle(particle: JumpParticle, partialTicks: Float) {
        with(mc.gameRenderer.camera.pos) {
            matrixStack.translate(-x, -y, -z)
        }

        val interpolated = particle.pos.interpolate(particle.prevPos, partialTicks.toDouble())
        matrixStack.translate(interpolated.x, interpolated.y, interpolated.z)

        val progress = ((System.currentTimeMillis() - particle.spawnTime) / 1000f) / JumpParticles.particleLifetime
        val easedProgress = animCurve.transform(progress.coerceIn(0f, 1f))

        val size = JumpParticles.particleSize * 0.25f * (1f - easedProgress)
        val rotation = if (JumpParticles.rotate) (particle.rotation + easedProgress * 90f) % 360f else 0f

        matrixStack.translate(-size / 2.0, -size / 2.0, 0.0)
        matrixStack.multiply(mc.gameRenderer.camera.rotation)
        matrixStack.scale(-1.0f, 1.0f, -1.0f)
        matrixStack.multiply(Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, rotation))
        matrixStack.translate(size / 2.0, size / 2.0, 0.0)

        val color = animateParticleColor(
            outerColor,
            innerColor,
            easedProgress,
            hueOffsetAnim
        ).alpha(MathHelper.clamp((particle.alpha * 255).toInt(), 0, 255))

        drawCustomMesh(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR, ShaderProgramKeys.POSITION_TEX_COLOR) { matrix ->
            vertex(matrix, 0.0f, -size, 0.0f).texture(0.0f, 0.0f).color(color.toARGB())
            vertex(matrix, -size, -size, 0.0f).texture(0.0f, 1.0f).color(color.toARGB())
            vertex(matrix, -size, 0.0f, 0.0f).texture(1.0f, 1.0f).color(color.toARGB())
            vertex(matrix, 0.0f, 0.0f, 0.0f).texture(1.0f, 0.0f).color(color.toARGB())
        }
    }

    private fun animateParticleColor(
        startColor: Color4b,
        endColor: Color4b,
        progress: Float,
        hueOffset: Int
    ): Color4b {
        val fade = 1.0f - progress
        val r = (startColor.r * fade + endColor.r * progress).toInt().coerceIn(0, 255)
        val g = (startColor.g * fade + endColor.g * progress).toInt().coerceIn(0, 255)
        val b = (startColor.b * fade + endColor.b * progress).toInt().coerceIn(0, 255)
        val a = (startColor.a * fade + endColor.a * progress).toInt().coerceIn(0, 255)
        var result = Color4b(r, g, b, a)
        if (hueOffset != 0) result = shiftHue(result, (hueOffset * progress).toInt())
        return result
    }

    private inline val Vec3d.isBlockAir: Boolean
        get() = world.getBlockState(this.toBlockPos()).isAir
}
