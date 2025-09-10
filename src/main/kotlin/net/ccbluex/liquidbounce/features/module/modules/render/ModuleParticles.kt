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
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeePointFrom
import net.ccbluex.liquidbounce.utils.block.collisionShape
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.copy
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

/**
 * Particles
 *
 * Displays particles when attacking an entity.
 *
 * @author sqlerrorthing
 */
object ModuleParticles : ClientModule("Particles", category = Category.RENDER) {

    private val particleSize by float("Size", 1f, 0.5f..2f)
    private val count by intRange("Count", 2..10, 2..30, "particles")
    private val rotate by boolean("RandomParticleRotation", true)
    private class Physical : Configurable("Physical") {
        val motion by float("Motion", 15f, 1f..30f)
        val bounceX by float("BounceX", 0.8f, 0.0f..1.0f)
        val bounceY by float("BounceY", 0.6f, 0.0f..1.0f)
        val bounceZ by float("BounceZ", 0.8f, 0.0f..1.0f)
        val drag by float("Drag", 0.99f, 0.0f..1.0f)
        val gravityFactor by float("GravityFactor", 0.8f, 0.0f..1f)
    }

    private val physicalSettings = Physical()
    init {
        tree(physicalSettings)
    }

    private val color by color("Color", Color4b.RED)
    private val particleImages by multiEnumChoice("Particle", ParticleImage.STAR, canBeNone = false)
    private val particles = mutableListOf<Particle>()
    private val chronometer = Chronometer()

    private val gravity: Double
        get() = physicalSettings.gravityFactor.toDouble() * 0.03125

    override fun onDisabled() {
        particles.clear()
        super.onDisabled()
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        particles.clear()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val camera = mc.cameraEntity ?: player
        particles.removeIf { particle ->
            if (particle.alpha <= 0 || camera.eyePos.squaredDistanceTo(particle.pos) > 30 * 30) {
                true
            } else {
                particle.update(camera.eyePos)
                false
            }
        }
    }

    @Suppress("unused")
    private val attackEvent = handler<AttackEntityEvent> { event ->
        if (!event.entity.shouldBeShown() || !chronometer.hasElapsed(230)) {
            return@handler
        }

        chronometer.reset()

        val directionVector = (RotationManager.currentRotation ?: player.rotation).directionVector
        val pos = player.eyePos.add(directionVector * player.distanceTo(event.entity).toDouble())

        repeat(count.random()) {
            particles.add(Particle(pos, particleImages.random()))
        }
    }

    @Suppress("unused")
    private val displayHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            RenderSystem.depthMask(true)
            RenderSystem.disableCull()
            mc.gameRenderer.lightmapTextureManager.disable()
            RenderSystem.defaultBlendFunc()

            for (particle in particles) {
                if (!particle.visible) continue

                particle.render(event.partialTicks)
            }

            RenderSystem.depthMask(true)
            RenderSystem.enableCull()
            RenderSystem.defaultBlendFunc()
            mc.gameRenderer.lightmapTextureManager.enable()
        }
    }


    @Suppress("UNUSED")
    private enum class ParticleImage(
        override val choiceName: String,
        val texture: Identifier
    ) : NamedChoice {
        /**
         * Original: IDK (first: https://github.com/CCBlueX/LiquidBounce/pull/4976)
         */
        ORBIZ("Orbiz", "particles/glow.png".registerAsDynamicImageFromClientResources()),

        /**
         * Original: https://www.svgrepo.com/svg/528677/stars-minimalistic
         * Modified: @sqlerrorthing
         */
        STAR("Star", "particles/star.png".registerAsDynamicImageFromClientResources()),

        /**
         * Original: https://www.svgrepo.com/svg/487288/dollar?edit=true
         * Modified: @sqlerrorthing
         */
        DOLLAR("Dollar", "particles/dollar.png".registerAsDynamicImageFromClientResources())
    }

    private class Particle(var pos: Vec3d, val particleImage: ParticleImage) {
        private var prevPos = pos
        private var velocity = Vec3d(
            (-0.01..0.01).random(),
            (0.01..0.02).random(),
            (-0.01..0.01).random()
        )
        @JvmField var alpha = 1f
        @JvmField var visible = true
        private val rotation = (0f..360f).random()
        private val spawnTime = System.currentTimeMillis()
        private var collisionTime = -1L

        fun update(cameraPos: Vec3d) {
            prevPos = pos

            if (collisionTime != -1L) {
                val timeSinceCollision = System.currentTimeMillis() - collisionTime
                alpha = max(0f, 1f - (timeSinceCollision / 3000f))
            }

            val speedMultiplier = physicalSettings.motion.toDouble()
            velocity = velocity.add(0.0, -gravity, 0.0)
            var nextPos = pos.add(velocity.multiply(speedMultiplier, 1.0, speedMultiplier))

            if (!nextPos.toBlockPos().collisionShape.isEmpty) {
                if (collisionTime == -1L) {
                    collisionTime = System.currentTimeMillis()
                }

                when {
                    !pos.toBlockPos(xOffset = velocity.x * speedMultiplier).collisionShape.isEmpty -> {
                        velocity = velocity.copy(x = -velocity.x * physicalSettings.bounceX)
                    }
                    !pos.toBlockPos(yOffset = velocity.y).collisionShape.isEmpty -> {
                        velocity = velocity.copy(
                            x = velocity.x * physicalSettings.drag,
                            y = -velocity.y * physicalSettings.bounceY,
                            z = velocity.z * physicalSettings.drag
                        )
                    }
                    !pos.toBlockPos(zOffset = velocity.z * speedMultiplier).collisionShape.isEmpty -> {
                        velocity = velocity.copy(z = -velocity.z * physicalSettings.bounceZ)
                    }
                }

                nextPos = pos.add(velocity.multiply(speedMultiplier, 1.0, speedMultiplier))
            }

            pos = nextPos
            visible = canSeePointFrom(cameraPos, pos)
        }

        context(env: WorldRenderEnvironment)
        fun render(partialTicks: Float) {
            val interpPos = prevPos.lerp(pos, partialTicks.toDouble())
            env.withPositionRelativeToCamera(interpPos) {
                RenderSystem.setShaderTexture(0, particleImage.texture)

                val size = particleSize * 0.25f * (1 - (System.currentTimeMillis() - spawnTime) / 12000f)
                val rotation = if (rotate) {
                    (rotation + 90f) % 360f
                } else {
                    90f
                }

                with(matrixStack) {
                    translate(-size / 2.0, -size / 2.0, 0.0)
                    multiply(mc.gameRenderer.camera.rotation)
                    scale(-1.0f, 1.0f, -1.0f)
                    multiply(Quaternionf().fromAxisAngleDeg(0.0f, 0.0f, 1.0f, rotation))
                    translate(size / 2.0, size / 2.0, 0.0)
                }

                val renderColor = color.alpha(
                    MathHelper.clamp(
                        (alpha * color.a.toFloat()).toInt(),
                        0, color.a
                    )
                )

                drawCustomMesh(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    ShaderProgramKeys.POSITION_TEX_COLOR
                ) { matrix ->
                    vertex(matrix, 0.0f, -size, 0.0f)
                        .texture(0.0f, 0.0f)
                        .color(renderColor.toARGB())

                    vertex(matrix, -size, -size, 0.0f)
                        .texture(0.0f, 1.0f)
                        .color(renderColor.toARGB())

                    vertex(matrix, -size, 0.0f, 0.0f)
                        .texture(1.0f, 1.0f)
                        .color(renderColor.toARGB())

                    vertex(matrix, 0.0f, 0.0f, 0.0f)
                        .texture(1.0f, 0.0f)
                        .color(renderColor.toARGB())
                }
            }
        }
    }

}
