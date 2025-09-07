package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.registerAsDynamicImageFromClientResources
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.math.RotationAxis
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object ModuleFireFlies : ClientModule("FireFlies", Category.RENDER) {

    private val fireFliesTexture = "image/firepart.png".registerAsDynamicImageFromClientResources()
    private val darkImprint by boolean("DarkImprint", true)
    private val lighting by boolean("Lighting", true)
    private val spawnDelay by float("SpawnDelay", 3.0f, 1.0f..10.0f)
    private val particleCount by int("ParticleCount", 50, 10..100)
    private val trailLength by int("TrailLength", 20, 5..50)
    private val colorMode = choices(this, "ColorMode") {
        arrayOf(
            GenericSyncColorMode(it),
            GenericCustomColorMode(it, Color4b.LIQUID_BOUNCE, Color4b.CYAN),
            GenericStaticColorMode(it, Color4b(255, 255, 100, 255)),
            GenericRainbowColorMode(it)
        )
    }

    private val particles = ArrayDeque<FireFly>()
    private val random = Random()

    private data class FireFly(
        var pos: Vec3d,
        var prevPos: Vec3d,
        val creationTime: Long,
        val maxAlive: Long,
        var velocity: Vec3d,
        val trail: MutableList<TrailPart> = mutableListOf()
    )

    private data class TrailPart(
        val pos: Vec3d,
        val creationTime: Long,
        val maxTime: Int
    ) {
        fun timePC(currentTime: Long): Float {
            return ((currentTime - creationTime).toFloat() / maxTime.toFloat()).coerceIn(0f, 1f)
        }

        fun toRemove(currentTime: Long): Boolean {
            return timePC(currentTime) >= 1.0f
        }
    }

    override fun onEnabled() {
        particles.clear()
    }

    override fun onDisabled() {
        particles.clear()
    }

    private fun generateRandomVelocity(): Vec3d {
        val yaw = random.nextDouble() * 360.0
        val speed = random.nextDouble() * 0.15 + 0.1
        val motionX = -sin(Math.toRadians(yaw)) * speed
        val motionZ = cos(Math.toRadians(yaw)) * speed
        val motionY = random.nextDouble() * 0.2 - 0.1
        return Vec3d(motionX, motionY, motionZ)
    }

    private fun generateSpawnPosition(): Vec3d {
        val player = mc.player ?: return Vec3d.ZERO
        val rangeXZ = 10.0
        val rangeY = 4.0
        val x = player.x + (random.nextDouble() - 0.5) * 2 * rangeXZ
        val y = player.y + (random.nextDouble() - 0.5) * rangeY
        val z = player.z + (random.nextDouble() - 0.5) * 2 * rangeXZ
        return Vec3d(x, y, z)
    }

    @Suppress("unused")
    val updateHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        val currentTime = mc.world?.time ?: return@handler

        if (currentTime % (spawnDelay.toInt() + 1) == 0L && particles.size < particleCount) {
            repeat(2) {
                val pos = generateSpawnPosition()
                particles.add(FireFly(pos, pos, currentTime, 6000L, generateRandomVelocity()))
            }
        }

        particles.forEach { particle ->
            particle.prevPos = particle.pos
            particle.pos = particle.pos.add(particle.velocity)
            particle.trail.add(TrailPart(particle.pos, currentTime, 400))
            particle.trail.removeIf { it.toRemove(currentTime) }
            if (particle.trail.size > trailLength) {
                particle.trail.removeAt(0)
            }
            if (random.nextFloat() < 0.05f) {
                particle.velocity = generateRandomVelocity()
            }
        }

        particles.removeIf { currentTime - it.creationTime > it.maxAlive }
    }

    @Suppress("unused")
    val renderHandler = handler<WorldRenderEvent> { event ->
        if (particles.isEmpty()) return@handler
        val player = mc.player ?: return@handler

        val currentTime = mc.world?.time ?: return@handler
        val matrixStack = event.matrixStack
        val partialTicks = event.partialTicks

        renderEnvironmentForWorld(matrixStack) {
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                if (darkImprint) GlStateManager.DstFactor.ONE
                else GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE
            )
            RenderSystem.disableCull()
            RenderSystem.disableDepthTest()
            RenderSystem.lineWidth(2.0f)

            val tess = RenderSystem.renderThreadTesselator()
            val buffer = tess.begin(VertexFormat.DrawMode.LINE_STRIP, VertexFormats.POSITION_COLOR)
            val camera = mc.gameRenderer.camera

            particles.forEach { particle ->
                val distance = player.pos.squaredDistanceTo(particle.pos.x, particle.pos.y, particle.pos.z).let { sqrt(it) }
                if (distance > 25) return@forEach

                val (color1, color2) = colorMode.activeChoice.getColors(player)
                val t = (sin(currentTime.toFloat() * 0.2f) + 1) * 0.5f
                val baseColor = color1.blend(color2, t)
                val progress = 1f - ((currentTime - particle.creationTime).toFloat() / particle.maxAlive.toFloat()).coerceIn(0f, 1f)

                particle.trail.forEach { trail ->
                    val trailColor = baseColor.withAlpha((baseColor.a * (1f - trail.timePC(currentTime)) * progress).toInt())
                    val x = (trail.pos.x - camera.pos.x).toFloat()
                    val y = (trail.pos.y - camera.pos.y).toFloat()
                    val z = (trail.pos.z - camera.pos.z).toFloat()
                    buffer.vertex(matrixStack.peek().positionMatrix, x, y, z)
                        .color(trailColor.r / 255f, trailColor.g / 255f, trailColor.b / 255f, trailColor.a / 255f)
                }
            }

            val v = buffer.endNullable() ?: return@renderEnvironmentForWorld
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
            BufferRenderer.drawWithGlobalProgram(v)

            // Render particles
            RenderSystem.setShaderTexture(0, fireFliesTexture)
            var index = 0
            particles.forEach { particle ->
                index++
                val interpPos = particle.prevPos.lerp(particle.pos, partialTicks.toDouble())
                val x = interpPos.x - camera.pos.x
                val y = interpPos.y - camera.pos.y
                val z = interpPos.z - camera.pos.z

                val distance = player.pos.squaredDistanceTo(particle.pos.x, particle.pos.y, particle.pos.z).let { sqrt(it) }
                if (distance > 25) return@forEach

                val progress = 1f - ((currentTime - particle.creationTime).toFloat() / particle.maxAlive.toFloat()).coerceIn(0f, 1f)
                matrixStack.push()
                matrixStack.translate(x, y, z)
                val size = 0.1f + 0.05f * (1f - progress)
                matrixStack.scale(size, size, size)
                matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.yaw))
                matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))

                val (color1, color2) = colorMode.activeChoice.getColors(player)
                val t = (sin(currentTime.toFloat() * 0.2f + -(index / particles.size.toFloat()) * 13) + 1) * 0.5f
                val color = color1.blend(color2, t)
                val alpha = (color.a * progress).toInt()
                val renderColor = color.withAlpha(alpha)

                drawCustomMesh(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    ShaderProgramKeys.POSITION_TEX_COLOR
                ) { mat ->
                    vertex(mat, -0.5f, -0.5f, 0f).texture(0f, 0f).color(renderColor.toARGB())
                    vertex(mat, 0.5f, -0.5f, 0f).texture(1f, 0f).color(renderColor.toARGB())
                    vertex(mat, 0.5f, 0.5f, 0f).texture(1f, 1f).color(renderColor.toARGB())
                    vertex(mat, -0.5f, 0.5f, 0f).texture(0f, 1f).color(renderColor.toARGB())
                }

                if (lighting) {
                    val lightingSize = size * 3.0f
                    matrixStack.scale(3.0f, 3.0f, 3.0f)
                    val lightingColor = renderColor.withAlpha((alpha / 5))
                    drawCustomMesh(
                        VertexFormat.DrawMode.QUADS,
                        VertexFormats.POSITION_TEXTURE_COLOR,
                        ShaderProgramKeys.POSITION_TEX_COLOR
                    ) { mat ->
                        vertex(mat, -0.5f, -0.5f, 0f).texture(0f, 0f).color(lightingColor.toARGB())
                        vertex(mat, 0.5f, -0.5f, 0f).texture(1f, 0f).color(lightingColor.toARGB())
                        vertex(mat, 0.5f, 0.5f, 0f).texture(1f, 1f).color(lightingColor.toARGB())
                        vertex(mat, -0.5f, 0.5f, 0f).texture(0f, 1f).color(lightingColor.toARGB())
                    }
                }

                matrixStack.pop()
            }

            RenderSystem.enableDepthTest()
            RenderSystem.enableCull()
            RenderSystem.lineWidth(1.0f)
        }
    }
}
