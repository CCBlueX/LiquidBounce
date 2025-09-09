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
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

object ModuleFireFlies : ClientModule("FireFlies", Category.RENDER) {
    private val colorMode = choices(this, "ColorMode") {
        arrayOf(
            GenericSyncColorMode(it),
            GenericCustomColorMode(it, Color4b.LIQUID_BOUNCE, Color4b.CYAN),
            GenericStaticColorMode(it, Color4b.YELLOW),
            GenericRainbowColorMode(it)
        )
    }

    private val darkImprint by boolean("DarkImprint", true)
    private val lighting by boolean("Lighting", true)
    private val spawnDelay by float("SpawnDelay", 2.0f, 1.0f..10.0f)
    private val particleCount by int("ParticleCount", 64, 10..1000)
    private val maxLiveCount by int("MaxLiveCount", 500, 1..2000)

    private val fireFliesTexture = "image/firepart.png".registerAsDynamicImageFromClientResources()
    private val particles = ArrayDeque<FireFly>()
    private val random = Random()
    private var spawnTimer = 0f

    private data class FireFly(
        var pos: Vec3d,
        var prevPos: Vec3d,
        val creationTime: Long,
        val maxAlive: Long,
        var velocity: Vec3d,
        val trail: MutableList<TrailPart> = mutableListOf(),
        val phase: Float = random.nextFloat() * 13f
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
    private val updateHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        val currentTime = mc.world?.time ?: return@handler

        particles.forEach { particle ->
            particle.prevPos = particle.pos
            particle.pos = particle.pos.add(particle.velocity)
            particle.trail.add(TrailPart(particle.pos, currentTime, 400))
            particle.trail.removeIf { it.toRemove(currentTime) }
            if (random.nextFloat() < 0.05f) particle.velocity = generateRandomVelocity()
        }

        val maxDistance = 25.0
        particles.removeIf { particle ->
            val distanceSq = player.pos.squaredDistanceTo(particle.pos)
            distanceSq > maxDistance * maxDistance || currentTime - particle.creationTime > particle.maxAlive
        }

        spawnTimer += 1f
        if (spawnTimer >= spawnDelay) {
            spawnTimer = 0f
            val canSpawn = (maxLiveCount - particles.size).coerceAtLeast(0)
            repeat(particleCount.coerceAtMost(canSpawn)) {
                val pos = generateSpawnPosition()
                particles.add(FireFly(pos, pos, currentTime, 6000L, generateRandomVelocity()))
            }
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (particles.isEmpty()) return@handler
        val player = mc.player ?: return@handler
        val currentTime = mc.world?.time ?: return@handler
        val matrixStack = event.matrixStack
        val partialTicks = event.partialTicks

        renderEnvironmentForWorld(matrixStack) {
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SrcFactor.SRC_ALPHA,
                if (darkImprint) GlStateManager.DstFactor.ONE else GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SrcFactor.ZERO,
                GlStateManager.DstFactor.ONE
            )
            RenderSystem.disableCull()
            RenderSystem.disableDepthTest()

            RenderSystem.setShaderTexture(0, fireFliesTexture)

            val camera = mc.gameRenderer.camera
            val (color1Base, color2Base) = colorMode.activeChoice.getColors(player)
            val total = particles.size
            if (total == 0) {
                RenderSystem.enableDepthTest()
                RenderSystem.enableCull()
                return@renderEnvironmentForWorld
            }

            val time = currentTime.toFloat()
            val maxDistSq = 25.0 * 25.0

            val yawRad = Math.toRadians(camera.yaw.toDouble())
            val pitchRad = Math.toRadians(camera.pitch.toDouble())
            val forward = Vec3d(
                -sin(yawRad) * cos(pitchRad),
                -sin(pitchRad),
                cos(yawRad) * cos(pitchRad)
            ).normalize()

            val worldUp = Vec3d(0.0, 1.0, 0.0)
            var rightVec = forward.crossProduct(worldUp)
            rightVec = if (rightVec.lengthSquared() == 0.0) Vec3d(1.0, 0.0, 0.0) else rightVec.normalize()
            val upAdjusted = rightVec.crossProduct(forward).normalize()

            val rxx = rightVec.x.toFloat(); val rxy = rightVec.y.toFloat(); val rxz = rightVec.z.toFloat()
            val uxx = upAdjusted.x.toFloat(); val uxy = upAdjusted.y.toFloat(); val uxz = upAdjusted.z.toFloat()

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { mat ->
                var index = 0
                val iter = particles.iterator()
                while (iter.hasNext()) {
                    index++
                    val particle = iter.next()

                    val interp = particle.prevPos.lerp(particle.pos, partialTicks.toDouble())
                    val cx = (interp.x - camera.pos.x).toFloat()
                    val cy = (interp.y - camera.pos.y).toFloat()
                    val cz = (interp.z - camera.pos.z).toFloat()

                    val dx = interp.x - player.x
                    val dy = interp.y - player.y
                    val dz = interp.z - player.z
                    val distSq = dx * dx + dy * dy + dz * dz
                    if (distSq > maxDistSq) continue

                    val progress = 1f - ((currentTime - particle.creationTime).toFloat() / particle.maxAlive.toFloat()).coerceIn(0f, 1f)
                    val t = (sin(time * 0.2f + particle.phase) + 1f) * 0.5f
                    val color = color1Base.blend(color2Base, t)
                    val alpha = (color.a * progress).toInt()
                    if (alpha <= 0) continue
                    val renderColor = color.withAlpha(alpha)

                    val sizes = if (lighting) floatArrayOf(0.1f + 0.05f * (1f - progress), 3f * (0.1f + 0.05f * (1f - progress)))
                    else floatArrayOf(0.1f + 0.05f * (1f - progress))
                    val alphas = if (lighting) intArrayOf(alpha, (alpha / 5).coerceAtLeast(1)) else intArrayOf(alpha)

                    sizes.forEachIndexed { idx, size ->
                        val half = size * 0.5f
                        val argb = renderColor.withAlpha(alphas[idx]).toARGB()

                        val rx = rxx * half; val ry = rxy * half; val rz = rxz * half
                        val ux = uxx * half; val uy = uxy * half; val uz = uxz * half

                        val v0x = cx - rx - ux; val v0y = cy - ry - uy; val v0z = cz - rz - uz
                        val v1x = cx + rx - ux; val v1y = cy + ry - uy; val v1z = cz + rz - uz
                        val v2x = cx + rx + ux; val v2y = cy + ry + uy; val v2z = cz + rz + uz
                        val v3x = cx - rx + ux; val v3y = cy - ry + uy; val v3z = cz - rz + uz

                        vertex(mat, v0x, v0y, v0z).texture(0f, 1f).color(argb)
                        vertex(mat, v1x, v1y, v1z).texture(1f, 1f).color(argb)
                        vertex(mat, v2x, v2y, v2z).texture(1f, 0f).color(argb)
                        vertex(mat, v3x, v3y, v3z).texture(0f, 0f).color(argb)
                    }
                }
            }

            RenderSystem.enableDepthTest()
            RenderSystem.enableCull()
        }
    }

}
