package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleParticles.color
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleParticles.mc
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleParticles.particleSize
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleParticles.rotate
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleParticles.speed
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.canSeePointFrom
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.random
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

/**
 * Particles
 *
 * Displays particles when attacking an entity.
 *
 * @author sqlerrorthing
 */
@Suppress("MagicNumber")
object ModuleParticles : ClientModule("Particles", category = Category.RENDER) {

    val particleSize by float("Size", 1f, 0.5f..2f)
    val speed by float("Speed", 1f, 0.5f..2f)
    private val count by intRange("Count", 2..10, 2..30, "particles")
    val rotate by boolean("RandomParticleRotation", true)
    val color by color("Color", Color4b.RED)

    private val particleImages by multiEnumChoice("Particle",
        ParticleImage.STAR,
        canBeNone = false
    )

    private val particles = mutableListOf<Particle>()
    private val chronometer = Chronometer()

    @Suppress("unused")
    private val attackEvent = handler<AttackEntityEvent> { event ->
        if (!event.entity.shouldBeShown() || !chronometer.hasElapsed(230) || event.isCancelled) {
            return@handler
        }

        chronometer.reset()

        val directionVector = (RotationManager.currentRotation ?: player.rotation).directionVector
        val pos = player.eyePos.add(directionVector * player.distanceTo(event.entity).toDouble())

        repeat(count.random()) { _ ->
            particles.add(Particle(pos, particleImages.random()))
        }
    }

    @Suppress("unused", "MagicNumber")
    private val displayHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            RenderSystem.depthMask(true)
            RenderSystem.disableCull()
            mc.gameRenderer.lightmapTextureManager.disable()
            RenderSystem.defaultBlendFunc()

            particles.removeIf { particle ->
                val flag = particle.alpha <= 0 || player.pos.distanceTo(particle.pos) > 30
                if (!flag) {
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

                return@removeIf flag
            }

            RenderSystem.depthMask(true)
            RenderSystem.enableCull()
            RenderSystem.defaultBlendFunc()
            mc.gameRenderer.lightmapTextureManager.enable()
        }
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

@Suppress("MagicNumber", "LongParameterList")
private class Particle private constructor(
    var pos: Vec3d,
    var prevPos: Vec3d,
    var velocity: Vec3d,
    var collisionTime: Long = -1,
    var alpha: Float = 1.0f, /* 0 <= alpha <= 1 */
    val spawnTime: Long = System.currentTimeMillis(),
    val rotation: Float,
    val particleImage: ParticleImage
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
        particleImage = particleImage
    )
}

@Suppress("MagicNumber", "UnusedParameter")
private fun Particle.update(delta: Double) {
    val particleSpeed = speed.toDouble()
    prevPos = pos

    if (collisionTime != -1L) {
        val timeSinceCollision = System.currentTimeMillis() - collisionTime
        alpha = max(0f, 1f - (timeSinceCollision / 3000f))
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

        if (!Vec3d(pos.x + dx, pos.y, pos.z).isBlockAir) {
            velocity = Vec3d(0.0, velocity.y, velocity.z)
        }

        if (!Vec3d(pos.x, pos.y + dy, pos.z).isBlockAir) {
            velocity = Vec3d(velocity.x, -velocity.y * 0.5, velocity.z)
        }

        if (!Vec3d(pos.x, pos.y, pos.z + dz).isBlockAir) {
            velocity = Vec3d(velocity.x, velocity.y, 0.0)
        }

        pos = pos.add((velocity * delta).multiply(particleSpeed, 1.0, particleSpeed))
    } else {
        pos = nextPos
    }
}

@Suppress("MagicNumber", "UnusedParameter")
private fun WorldRenderEnvironment.render(particle: Particle, partialTicks: Float) {
    with(mc.gameRenderer.camera.pos) {
        matrixStack.translate(-this.x, -this.y, -this.z)
    }

    with(particle.pos.interpolate(particle.prevPos, partialTicks.toDouble())) {
        matrixStack.translate(x, y, z)
    }

    val size = particleSize * 0.25f * (1 - (System.currentTimeMillis() - particle.spawnTime) / 12000f)
    val rotation = if (rotate) {
        (particle.rotation + 90f) % 360f
    } else {
        90f
    }

    with (matrixStack) {
        translate(-size / 2.0, -size / 2.0, 0.0)
        multiply(mc.gameRenderer.camera.rotation)
        scale(-1.0f, 1.0f, -1.0f)
        multiply(Quaternionf().fromAxisAngleDeg(0.0f, 0.0f, 1.0f, rotation))
        translate(size / 2.0, size / 2.0, 0.0)
    }

    val renderColor = color.alpha(MathHelper.clamp((particle.alpha * color.a.toFloat()).toInt(), 0, color.a))

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

inline val Vec3d.isBlockAir: Boolean get() =
    world.getBlockState(this.toBlockPos()).isAir
