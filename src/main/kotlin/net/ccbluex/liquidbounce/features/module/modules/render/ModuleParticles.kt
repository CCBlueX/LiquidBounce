package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
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
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.ccbluex.liquidbounce.utils.client.world
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
@Suppress("MagicNumber","UNUSED")
object ModuleParticles : ClientModule("Particles", category = Category.RENDER) {

    private val particleSize by float("Size", 1f, 0.5f..2f)
    private val count by intRange("Count", 2..10, 2..30, "particles")
    private val rotate by boolean("RandomParticleRotation", true)
    private class Physical : Configurable("Physical") {
        val motion by float("Motion", 15f, 1f..30f)
        val bounceX by float("Bounce X", 0.8f, 0.0f..1.0f)
        val bounceY by float("Bounce Y", 0.6f, 0.0f..1.0f)
        val bounceZ by float("Bounce Z", 0.8f, 0.0f..1.0f)
        val drag by float("Drag", 0.99f, 0.0f..1.0f)
        val gravityFactor by float("GravityFactor", 0.5f, 0.0f..1f)
    }

    private val physicalSettings = Physical()
    init {
        tree(physicalSettings)
    }

    private val color by color("Color", Color4b.WHITE)
    private val particleImages by multiEnumChoice("Particle", ParticleImage.SNOWFLAKE, canBeNone = false)
    private val particles = mutableListOf<Particle>()
    private val chronometer = Chronometer()


    private val gravity: Double
        get() = physicalSettings.gravityFactor.toDouble() * 0.0015

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

    private val displayHandler = handler<WorldRenderEvent> { event ->
        renderEnvironmentForWorld(event.matrixStack) {
            RenderSystem.depthMask(true)
            RenderSystem.disableCull()
            mc.gameRenderer.lightmapTextureManager.disable()
            RenderSystem.defaultBlendFunc()

            val camera = mc.cameraEntity ?: return@renderEnvironmentForWorld
            val now = System.currentTimeMillis()

            particles.removeIf { particle ->
                val expired = particle.alpha <= 0 || player.pos.distanceTo(particle.pos) > 30
                if (expired) return@removeIf true

                particle.update()

                if (now >= particle.nextVisibilityCheck) {
                    particle.visible = canSeePointFrom(camera.eyePos, particle.pos)
                    particle.nextVisibilityCheck = now + 50L
                }

                false
            }

            particles.filter { it.visible }.forEach { particle ->
                val interpPos = particle.prevPos.lerp(particle.pos, event.partialTicks.toDouble())
                withPositionRelativeToCamera(interpPos) {
                    val ms = event.matrixStack
                    ms.push()
                    RenderSystem.setShaderTexture(0, particle.particleImage.texture)
                    render(particle, event.partialTicks)
                    ms.pop()
                }
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
        SNOWFLAKE("Snowflake", "particles/snowflake.png".registerAsDynamicImageFromClientResources()),

        /**
         * Original: https://www.svgrepo.com/svg/487288/dollar?edit=true
         * Modified: @sqlerrorthing
         */
        DOLLAR("Dollar", "particles/dollar.png".registerAsDynamicImageFromClientResources())
    }

    private class Particle(
        var pos: Vec3d,
        var prevPos: Vec3d,
        var velocity: Vec3d,
        var collisionTime: Long = -1,
        var alpha: Float = 1f,
        val spawnTime: Long = System.currentTimeMillis(),
        val rotation: Float,
        val particleImage: ParticleImage,
        var visible: Boolean = true,
        var nextVisibilityCheck: Long = 0L
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

        fun update() {
            prevPos = pos

            if (collisionTime != -1L) {
                val timeSinceCollision = System.currentTimeMillis() - collisionTime
                alpha = max(0f, 1f - (timeSinceCollision / 3000f))
            }

            val speedMultiplier = physicalSettings.motion.toDouble()
            velocity = velocity.add(0.0, -gravity, 0.0)
            var nextPos = pos.add(velocity.multiply(speedMultiplier, 1.0, speedMultiplier))

            if (!nextPos.isBlockAir) {
                if (collisionTime == -1L) collisionTime = System.currentTimeMillis()

                if (!Vec3d(pos.x + velocity.x * speedMultiplier, pos.y, pos.z).isBlockAir) {
                    velocity = velocity.copy(x = -velocity.x * physicalSettings.bounceX)
                }
                if (!Vec3d(pos.x, pos.y + velocity.y, pos.z).isBlockAir) {
                    velocity = velocity.copy(
                        x = velocity.x * physicalSettings.drag,
                        y = -velocity.y * physicalSettings.bounceY,
                        z = velocity.z * physicalSettings.drag
                    )
                }
                if (!Vec3d(pos.x, pos.y, pos.z + velocity.z * speedMultiplier).isBlockAir) {
                    velocity = velocity.copy(z = -velocity.z * physicalSettings.bounceZ)
                }

                nextPos = pos.add(velocity.multiply(speedMultiplier, 1.0, speedMultiplier))
            }

            pos = nextPos
        }
    }

    private fun WorldRenderEnvironment.render(particle: Particle, partialTicks: Float) {
        val size = particleSize * 0.25f * (1 - (System.currentTimeMillis() - particle.spawnTime) / 12000f)
        val rotation = if (rotate) {
            (particle.rotation + 90f) % 360f
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
                (particle.alpha * color.a.toFloat()).toInt(),
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

inline val Vec3d.isBlockAir: Boolean
    get() =
        world.getBlockState(this.toBlockPos()).isAir
