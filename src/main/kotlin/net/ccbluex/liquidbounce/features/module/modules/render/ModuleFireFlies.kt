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
import java.util.*
import java.util.concurrent.ThreadLocalRandom
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
    private val spawnDelay by float("SpawnDelay", 2.0f, 1.0f..10.0f)
    private val spawnleCount by int("SpawnCount", 16, 10..10000)
    private val maxLiveCount by int("MaxLiveCount", 1337, 50..50000)
    private val maxQuadsPerFrame by int("MaxQuadsPerFrame",5000,5000..100000)

    private val fireFliesTexture = "image/firepart.png".registerAsDynamicImageFromClientResources()
    private val particles = ArrayDeque<FireFly>()

    private val rand get() = ThreadLocalRandom.current()
    private var spawnTimer = 0f

    private data class FloatVec(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
        fun setFrom(other: FloatVec) {
            x = other.x; y = other.y; z = other.z
        }
        fun addInplace(vx: Float, vy: Float, vz: Float) {
            x += vx; y += vy; z += vz
        }
    }

    private data class FireFly(
        val pos: FloatVec,
        val prevPos: FloatVec,
        val creationTime: Long,
        val maxAlive: Long,
        val velocity: FloatVec,
        val phase: Float = (ThreadLocalRandom.current().nextFloat() * 13f)
    )

    override fun onEnabled() {
        particles.clear()
    }

    override fun onDisabled() {
        particles.clear()
    }

    private fun generateRandomVelocity(): FloatVec {
        val yaw = rand.nextDouble() * Math.PI * 2.0
        val speed = (rand.nextDouble() * 0.15 + 0.1).toFloat()
        val motionX = (-sin(yaw) * speed).toFloat()
        val motionZ = (cos(yaw) * speed).toFloat()
        val motionY = (rand.nextDouble() * 0.2 - 0.1).toFloat()
        return FloatVec(motionX, motionY, motionZ)
    }

    private fun generateSpawnPosition(): FloatVec {
        val player = mc.player ?: return FloatVec()
        val rangeXZ = 10.0
        val rangeY = 4.0
        val x = player.x + (rand.nextDouble() - 0.5) * 2.0 * rangeXZ
        val y = player.y + (rand.nextDouble() - 0.5) * rangeY
        val z = player.z + (rand.nextDouble() - 0.5) * 2.0 * rangeXZ
        return FloatVec(x.toFloat(), y.toFloat(), z.toFloat())
    }

    @Suppress("unused")
    private val updateHandler = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        val currentTime = mc.world?.time ?: return@handler

        val it = particles.iterator()
        while (it.hasNext()) {
            val particle = it.next()

            particle.prevPos.setFrom(particle.pos)

            val vx = particle.velocity.x; val vy = particle.velocity.y; val vz = particle.velocity.z
            particle.pos.addInplace(vx, vy, vz)

            if (rand.nextFloat() < 0.05f) {
                val nv = generateRandomVelocity()
                particle.velocity.x = nv.x; particle.velocity.y = nv.y; particle.velocity.z = nv.z
            }
        }

        val playerX = player.x.toFloat(); val playerY = player.y.toFloat(); val playerZ = player.z.toFloat()
        val maxDistance = 25f
        val maxDistSq = maxDistance * maxDistance
        if (particles.isNotEmpty()) {
            val survivors = ArrayDeque<FireFly>(particles.size)
            val itr = particles.iterator()
            while (itr.hasNext()) {
                val p = itr.next()
                val dx = p.pos.x - playerX; val dy = p.pos.y - playerY; val dz = p.pos.z - playerZ
                val distSq = dx * dx + dy * dy + dz * dz
                if (distSq <= maxDistSq && (currentTime - p.creationTime) <= p.maxAlive) survivors.addLast(p)
            }
            if (survivors.size != particles.size) {
                particles.clear()
                particles.addAll(survivors)
            }
        }

        spawnTimer += 1f
        if (spawnTimer >= spawnDelay) {
            spawnTimer = 0f
            val canSpawn = (maxLiveCount - particles.size).coerceAtLeast(0)
            val spawnCount = spawnleCount.coerceAtMost(canSpawn)
            repeat(spawnCount) {
                val pos = generateSpawnPosition()
                val vel = generateRandomVelocity()
                particles.addLast(
                    FireFly(
                        FloatVec(pos.x, pos.y, pos.z),
                        FloatVec(pos.x, pos.y, pos.z),
                        currentTime,
                        6000L,
                        vel
                    )
                )
            }
        }
    }


    @Suppress("unused","LongParameterList")
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
            val maxDistSq = 25.0f * 25.0f

            val yawRad = Math.toRadians(camera.yaw.toDouble()).toFloat()
            val pitchRad = Math.toRadians(camera.pitch.toDouble()).toFloat()
            val forwardX = (-sin(yawRad.toDouble()) * cos(pitchRad.toDouble())).toFloat()
            val forwardY = (-sin(pitchRad.toDouble())).toFloat()
            val forwardZ = (cos(yawRad.toDouble()) * cos(pitchRad.toDouble())).toFloat()
            val fLen = kotlin.math.sqrt(forwardX * forwardX + forwardY * forwardY + forwardZ * forwardZ)
            val fx = forwardX / fLen; val fy = forwardY / fLen; val fz = forwardZ / fLen

            val uxw = 0f; val uyw = 1f; val uzw = 0f
            var rx = fy * uzw - fz * uyw
            var ry = fz * uxw - fx * uzw
            var rz = fx * uyw - fy * uxw
            var rlen = rx*rx + ry*ry + rz*rz
            if (rlen == 0f) {
                rx = 1f; ry = 0f; rz = 0f; rlen = 1f
            }
            rlen = kotlin.math.sqrt(rlen)
            rx /= rlen; ry /= rlen; rz /= rlen
            var ux = ry * fz - rz * fy
            var uy = rz * fx - rx * fz
            var uz = rx * fy - ry * fx
            val ulen = kotlin.math.sqrt(ux*ux + uy*uy + uz*uz)
            ux /= ulen; uy /= ulen; uz /= ulen

            val camX = camera.pos.x.toFloat(); val camY = camera.pos.y.toFloat(); val camZ = camera.pos.z.toFloat()
            val playerX = player.x.toFloat(); val playerY = player.y.toFloat(); val playerZ = player.z.toFloat()


            var rendered = 0

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR
            ) { mat ->

                fun emitQuadInline(
                    cx: Float, cy: Float, cz: Float,
                    halfRx: Float, halfRy: Float, halfRz: Float,
                    halfUx: Float, halfUy: Float, halfUz: Float,
                    argb: Int
                ) {
                    val v0x = cx - halfRx - halfUx
                    val v0y = cy - halfRy - halfUy
                    val v0z = cz - halfRz - halfUz
                    val v1x = cx + halfRx - halfUx
                    val v1y = cy + halfRy - halfUy
                    val v1z = cz + halfRz - halfUz
                    val v2x = cx + halfRx + halfUx
                    val v2y = cy + halfRy + halfUy
                    val v2z = cz + halfRz + halfUz
                    val v3x = cx - halfRx + halfUx
                    val v3y = cy - halfRy + halfUy
                    val v3z = cz - halfRz + halfUz

                    vertex(mat, v0x, v0y, v0z).texture(0f, 1f).color(argb)
                    vertex(mat, v1x, v1y, v1z).texture(1f, 1f).color(argb)
                    vertex(mat, v2x, v2y, v2z).texture(1f, 0f).color(argb)
                    vertex(mat, v3x, v3y, v3z).texture(0f, 0f).color(argb)
                }

                val itr = particles.iterator()
                while (itr.hasNext()) {
                    if (rendered >= maxQuadsPerFrame) break
                    val particle = itr.next()

                    // interpolate
                    val px = particle.prevPos.x
                    val py = particle.prevPos.y
                    val pz = particle.prevPos.z
                    val nx = particle.pos.x
                    val ny = particle.pos.y
                    val nz = particle.pos.z
                    val interpX = px + (nx - px) * partialTicks
                    val interpY = py + (ny - py) * partialTicks
                    val interpZ = pz + (nz - pz) * partialTicks

                    val cx = interpX - camX
                    val cy = interpY - camY
                    val cz = interpZ - camZ

                    val dx = interpX - playerX
                    val dy = interpY - playerY
                    val dz = interpZ - playerZ
                    val distSq = dx * dx + dy * dy + dz * dz
                    if (distSq > maxDistSq) continue

                    val progress = 1f - ((currentTime -
                            particle.creationTime).toFloat() / particle.maxAlive.toFloat()).coerceIn(0f, 1f)
                    val t = (sin(time * 0.2f + particle.phase) + 1f) * 0.5f

                    val blended = color1Base.blend(color2Base, t)
                    val rgbOnly = blended.toARGB() and 0x00FFFFFF

                    val alpha = (blended.a * progress).toInt()
                    if (alpha <= 0) continue

                    val outerArgb = ((alpha and 0xFF) shl 24) or rgbOnly

                    val sizeOuter = 0.1f + 0.05f * (1f - progress)

                    val half = sizeOuter * 0.5f
                    val hrx = rx * half; val hry = ry * half; val hrz = rz * half
                    val hux = ux * half; val huy = uy * half; val huz = uz * half

                    emitQuadInline(cx, cy, cz, hrx, hry, hrz, hux, huy, huz, outerArgb)
                    rendered++
                }
            }

            RenderSystem.enableDepthTest()
            RenderSystem.enableCull()
        }
    }

}
