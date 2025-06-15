package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.liquidbounce.config.types.NamedChoice
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
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeePointFrom
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
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable

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
        val particleImages by multiEnumChoice("Particle",
            *ParticleImage.entries.toTypedArray(),
            canBeNone = false
        )
        val particleLifetime by float("JumpParticleLifetime", 2.5f, 0.5f..5f)
    }

    private val circles = ArrayDeque<ObjectLongMutablePair<Vec3d>>()
    private val particles = ArrayList<Particle>()

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
                Particle(
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
                    val transparent = particle.alpha <= 0

                    if (!(expired || outOfRange || transparent)) {
                        particle.update(event.partialTicks.toDouble())
                        mc.cameraEntity?.let { camera ->
                            if (canSeePointFrom(camera.eyePos, particle.pos)) {
                                matrixStack.push()
                                RenderSystem.setShaderTexture(0, particle.particleImage.texture)
                                render(particle, event.partialTicks)
                                matrixStack.pop()
                            }
                        }
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

    enum class ParticleImage(
        override val choiceName: String,
        val texture: Identifier
    ) : NamedChoice {
        ORBIZ("Orbiz", "particles/glow.png".registerAsDynamicImageFromClientResources()),
        STAR("Star", "particles/star.png".registerAsDynamicImageFromClientResources()),
        DOLLAR("Dollar", "particles/dollar.png".registerAsDynamicImageFromClientResources())
    }

    private class Particle private constructor(
        var pos: Vec3d,
        var prevPos: Vec3d,
        var velocity: Vec3d,
        var collisionTime: Long = -1,
        var alpha: Float = 1.0f,
        val spawnTime: Long = System.currentTimeMillis(),
        val rotation: Float,
        val particleImage: ParticleImage,
        val innerColor: Color4b,
        val outerColor: Color4b
    ) {
        constructor(pos: Vec3d, particleImage: ParticleImage) : this(
            pos = pos,
            prevPos = pos,
            velocity = Vec3d(
                (-0.01..0.01).random(),
                (0.01..0.02).random(),
                (-0.01..0.01).random()
            ),
            rotation = (0f..360f).random(),
            particleImage = particleImage,
            innerColor = ModuleJumpEffect.innerColor,
            outerColor = ModuleJumpEffect.outerColor
        )
    }

    private fun Particle.update(delta: Double) {
        val particleSpeed = JumpParticles.speed.toDouble()
        prevPos = pos

        if (collisionTime != -1L) {
            val timeSinceCollision = (System.currentTimeMillis() - collisionTime) / 1000f
            alpha = max(0f, 1f - (timeSinceCollision / JumpParticles.particleLifetime))
        }

        velocity = velocity.add(0.0, -0.0001, 0.0)
        val nextPos = pos.add((velocity * delta).multiply(particleSpeed, 1.0, particleSpeed))

        if (!nextPos.isBlockAir) {
            if (collisionTime == -1L) {
                collisionTime = System.currentTimeMillis()
            }

            val dx = velocity.x * delta * particleSpeed
            val dy = velocity.y * delta
            val dz = velocity.z * delta * particleSpeed

            if (!Vec3d(pos.x + dx, pos.y, pos.z).isBlockAir)
                velocity = Vec3d(0.0, velocity.y, velocity.z)

            if (!Vec3d(pos.x, pos.y + dy, pos.z).isBlockAir)
                velocity = Vec3d(velocity.x, -velocity.y * 0.5, velocity.z)

            if (!Vec3d(pos.x, pos.y, pos.z + dz).isBlockAir)
                velocity = Vec3d(velocity.x, velocity.y, 0.0)

            pos = pos.add((velocity * delta).multiply(particleSpeed, 1.0, particleSpeed))
        } else {
            pos = nextPos
        }
    }

    private fun WorldRenderEnvironment.render(particle: Particle, partialTicks: Float) {
        with(mc.gameRenderer.camera.pos) {
            matrixStack.translate(-x, -y, -z)
        }

        val interpolated = particle.pos.interpolate(particle.prevPos, partialTicks.toDouble())
        matrixStack.translate(interpolated.x, interpolated.y, interpolated.z)

        val progress = ((System.currentTimeMillis() - particle.spawnTime) / 1000f) / JumpParticles.particleLifetime
        val easedProgress = animCurve.transform(progress.coerceIn(0f, 1f))

        val size = JumpParticles.particleSize * 0.25f * (1f - easedProgress)
        val rotation = if (JumpParticles.rotate) (particle.rotation + 90f) % 360f else 90f

        matrixStack.translate(-size / 2.0, -size / 2.0, 0.0)
        matrixStack.multiply(mc.gameRenderer.camera.rotation)
        matrixStack.scale(-1.0f, 1.0f, -1.0f)
        matrixStack.multiply(Quaternionf().fromAxisAngleDeg(0f, 0f, 1f, rotation))
        matrixStack.translate(size / 2.0, size / 2.0, 0.0)

        val flatColor = animateParticleColor(
            particle.outerColor,
            particle.innerColor,
            easedProgress,
            hueOffsetAnim
        ).alpha(MathHelper.clamp((particle.alpha * 255).toInt(), 0, 255))

        drawCustomMesh(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR, ShaderProgramKeys.POSITION_TEX_COLOR) { matrix ->
            vertex(matrix, 0.0f, -size, 0.0f).texture(0.0f, 0.0f).color(flatColor.toARGB())
            vertex(matrix, -size, -size, 0.0f).texture(0.0f, 1.0f).color(flatColor.toARGB())
            vertex(matrix, -size, 0.0f, 0.0f).texture(1.0f, 1.0f).color(flatColor.toARGB())
            vertex(matrix, 0.0f, 0.0f, 0.0f).texture(1.0f, 0.0f).color(flatColor.toARGB())
        }
    }

    private inline val Vec3d.isBlockAir: Boolean
        get() = world.getBlockState(this.toBlockPos()).isAir
}
